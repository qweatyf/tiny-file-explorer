package com.example.myempty.activity2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class GitHubUploadActivity extends AppCompatActivity {

    private EditText etToken, etOwner, etRepo, etBranch, etMsg, etDir, etExclude;
    private TextView tvTokenStatus, tvLog;
    private Button btnCheck, btnClear, btnPick, btnUpload;
    private ProgressBar pb;

    private File selectedDir;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_github_upload);

        etToken = findViewById(R.id.et_gh_token);
        etOwner = findViewById(R.id.et_gh_owner);
        etRepo = findViewById(R.id.et_gh_repo);
        etBranch = findViewById(R.id.et_gh_branch);
        etMsg = findViewById(R.id.et_gh_msg);
        etDir = findViewById(R.id.et_gh_dir);
        etExclude = findViewById(R.id.et_gh_exclude);
        tvTokenStatus = findViewById(R.id.tv_gh_token_status);
        tvLog = findViewById(R.id.tv_gh_log);
        btnCheck = findViewById(R.id.btn_gh_check);
        btnClear = findViewById(R.id.btn_gh_clear);
        btnPick = findViewById(R.id.btn_gh_pick);
        btnUpload = findViewById(R.id.btn_gh_upload);
        pb = findViewById(R.id.pb_gh);

        String saved = GitHubTokenStore.load(this);
        if (!saved.isEmpty()) {
            etToken.setText(saved);
            tvTokenStatus.setText(getString(R.string.github_token_saved));
        }

        btnCheck.setOnClickListener(v -> doCheck());

        etToken.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && etToken.getText().toString().trim().length() > 0) {
                doCheck();
            }
        });

        btnClear.setOnClickListener(v -> {
            GitHubTokenStore.clear(this);
            etToken.setText("");
            tvTokenStatus.setText("");
            Toast.makeText(this, getString(R.string.github_cleared),
                Toast.LENGTH_SHORT).show();
        });

        btnPick.setOnClickListener(v -> pickDir());
        btnUpload.setOnClickListener(v -> doUpload());
    }

    private void doCheck() {
        String token = etToken.getText().toString().trim();
        if (token.isEmpty()) {
            tvTokenStatus.setText(getString(R.string.github_token_empty));
            return;
        }
        GitHubTokenStore.save(this, token);
        tvTokenStatus.setText(getString(R.string.github_checking));
        new GitHubClient(token).checkToken((ok, msg) -> runOnUiThread(() -> {
            if (ok) {
                tvTokenStatus.setText(getString(R.string.github_token_ok, msg));
            } else {
                tvTokenStatus.setText(getString(R.string.github_token_fail, msg));
            }
        }));
    }

    private void pickDir() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, 200);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 200 && res == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            String path = uriToPath(uri);
            if (path != null) {
                etDir.setText(path);
                selectedDir = new File(path);
            } else {
                Toast.makeText(this, getString(R.string.github_dir_fail),
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private String uriToPath(Uri uri) {
        try {
            String docId = android.provider.DocumentsContract.getTreeDocumentId(uri);
            if (docId == null) return null;
            int colon = docId.indexOf(':');
            if (colon < 0) return null;
            String type = docId.substring(0, colon);
            String rel = docId.substring(colon + 1);
            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/" + rel;
            } else {
                return "/storage/" + type + "/" + rel;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void doUpload() {
        String token = etToken.getText().toString().trim();
        String owner = etOwner.getText().toString().trim();
        String repo = etRepo.getText().toString().trim();
        String branch = etBranch.getText().toString().trim();
        String msg = etMsg.getText().toString().trim();
        String dirPath = etDir.getText().toString().trim();
        String exclude = etExclude.getText().toString();

        if (token.isEmpty() || owner.isEmpty() || repo.isEmpty() || dirPath.isEmpty()) {
            Toast.makeText(this, getString(R.string.github_need_input),
                Toast.LENGTH_SHORT).show();
            return;
        }
        if (branch.isEmpty()) branch = "main";
        if (msg.isEmpty()) msg = "update";

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            Toast.makeText(this, getString(R.string.github_dir_fail),
                Toast.LENGTH_SHORT).show();
            return;
        }

        GitHubTokenStore.save(this, token);
        selectedDir = dir;

        pb.setVisibility(View.VISIBLE);
        pb.setProgress(0);
        tvLog.setText("");

        final String fOwner = owner;
        final String fRepo = repo;
        final String fBranch = branch;
        final String fMsg = msg;
        final String fExclude = exclude;

        new GitHubClient(token).uploadDir(dir, fOwner, fRepo, fBranch, fMsg, fExclude,
            new GitHubClient.Progress() {
                @Override
                public void onStep(String m) {
                    runOnUiThread(() -> appendLog(m));
                }

                @Override
                public void onFile(int cur, int total, String path) {
                    runOnUiThread(() -> {
                        appendLog("[" + cur + "/" + total + "] " + path);
                        pb.setProgress(cur * 100 / Math.max(1, total));
                    });
                }

                @Override
                public void onDone(String commitUrl) {
                    runOnUiThread(() -> {
                        pb.setProgress(100);
                        appendLog(getString(R.string.github_upload_done) + "\n" + commitUrl);
                        Toast.makeText(GitHubUploadActivity.this, commitUrl,
                            Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onFail(String err) {
                    runOnUiThread(() -> {
                        appendLog(getString(R.string.github_upload_fail) + err);
                        Toast.makeText(GitHubUploadActivity.this, err,
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
    }

    private void appendLog(String s) {
        tvLog.append(s + "\n");
    }
}