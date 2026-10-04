package com.example.myempty.activity2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QrScanActivity extends Activity {

    private TextView tvResult;
    private Button btnCopy;
    private Button btnOpen;
    private Button btnPick;
    private Button btnScan;

    private String lastText = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_qr_scan);

        tvResult = findViewById(R.id.tv_qr_scan_result);
        btnCopy = findViewById(R.id.btn_qr_scan_copy);
        btnOpen = findViewById(R.id.btn_qr_scan_open);
        btnPick = findViewById(R.id.btn_qr_scan_pick);
        btnScan = findViewById(R.id.btn_qr_scan_cam);

        btnCopy.setEnabled(false);
        btnOpen.setEnabled(false);

        btnScan.setOnClickListener(v -> {
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.CAMERA
                }, 100);
            } else {
                startCameraScan();
            }
        });

        btnPick.setOnClickListener(v -> pickImage());

        btnCopy.setOnClickListener(v -> {
            if (lastText.isEmpty()) return;
            ClipboardManager cm = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("qr", lastText));
            Toast.makeText(this, "拷贝了", Toast.LENGTH_SHORT).show();
        });

        btnOpen.setOnClickListener(v -> {
            if (lastText.isEmpty()) return;
            openByText(lastText, true);
        });
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == 100) {
            if (results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraScan();
            } else {
                Toast.makeText(this, "不给相机权限扫不了", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCameraScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("对着二维码怼准了");
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(QrCaptureActivity.class);
        integrator.initiateScan();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, 300);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        IntentResult r = IntentIntegrator.parseActivityResult(req, res, data);
        if (r != null && r.getContents() != null) {
            showResult(r.getContents());
            return;
        }

        if (req == 300 && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) decodeFromImage(uri);
            return;
        }

        super.onActivityResult(req, res, data);
    }

    private void decodeFromImage(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                getContentResolver(), uri);
            if (bmp == null) {
                Toast.makeText(this, "读不了这张图", Toast.LENGTH_SHORT).show();
                return;
            }

            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int[] px = new int[w * h];
            bmp.getPixels(px, 0, w, 0, 0, w, h);
            bmp.recycle();

            LuminanceSource src = new RGBLuminanceSource(w, h, px);
            BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(src));
            Result result = new MultiFormatReader().decode(bb);

            showResult(result.getText());
        } catch (Exception e) {
            Toast.makeText(this, "没识别到二维码，换张清楚的试试",
                Toast.LENGTH_LONG).show();
        }
    }

    private void showResult(String text) {
        lastText = text;
        tvResult.setText(text);
        btnCopy.setEnabled(true);
        btnOpen.setEnabled(true);

        if (isPaymentLink(text)) {
            showPayWarn(text);
        } else if (isNormalUrl(text)) {
            autoOpen(text);
        }
    }

    private boolean isPaymentLink(String s) {
        String low = s.toLowerCase();
        return low.startsWith("alipay://")
            || low.startsWith("weixin://")
            || low.startsWith("wechat://")
            || low.contains("qr.alipay.com")
            || low.contains("wx.tenpay.com")
            || low.contains("paypal.com")
            || low.contains("pay.")
            || low.contains("pay?")
            || low.contains("/pay/");
    }

    private boolean isNormalUrl(String s) {
        String low = s.toLowerCase();
        return (low.startsWith("http://") || low.startsWith("https://"))
            && !isPaymentLink(s);
    }

    private void showPayWarn(final String url) {
        new AlertDialog.Builder(this)
            .setTitle("小心")
            .setMessage("这玩意儿像支付链接，别乱点：\n\n" + url
                + "\n\n确定要打开吗？")
            .setPositiveButton("打开", (d, w) -> openByText(url, true))
            .setNegativeButton("算了", null)
            .show();
    }

    private void autoOpen(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "没装能打开它的 App", Toast.LENGTH_SHORT).show();
        }
    }

    private void openByText(String text, boolean open) {
        if (!open) return;
        String low = text.toLowerCase();
        if (low.startsWith("http://") || low.startsWith("https://")
            || low.startsWith("alipay://") || low.startsWith("weixin://")) {
            autoOpen(text);
        } else {
            Toast.makeText(this, "这不是网址，不跳了", Toast.LENGTH_SHORT).show();
        }
    }
}