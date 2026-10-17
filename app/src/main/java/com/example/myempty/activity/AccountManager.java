package com.example.myempty.activity2;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;

public class AccountManager {
    private static final String FILE_NAME = "account.txt";

    public static boolean hasAccount(Context ctx) {
        return new File(ctx.getFilesDir(), FILE_NAME).exists();
    }

    public static void saveAccount(Context ctx, String username, String cardHash) throws Exception {
        String data = "username=" + username + "\ncardHash=" + cardHash + "\n";
        FileOutputStream fos = new FileOutputStream(new File(ctx.getFilesDir(), FILE_NAME));
        fos.write(data.getBytes());
        fos.close();
    }

    public static void deleteAccount(Context ctx) {
        new File(ctx.getFilesDir(), FILE_NAME).delete();
    }
}