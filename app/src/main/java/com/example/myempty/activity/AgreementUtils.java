package com.example.myempty.activity2;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;

public class AgreementUtils {

    private static final String FILE_NAME = "agreed.txt";

    public static boolean hasAgreed(Context ctx) {
        return new File(ctx.getFilesDir(), FILE_NAME).exists();
    }

    public static void markAgreed(Context ctx) {
        try {
            FileOutputStream fos = new FileOutputStream(
                new File(ctx.getFilesDir(), FILE_NAME));
            fos.write("agreed".getBytes());
            fos.close();
        } catch (Exception ignored) {}
    }

    public static String getAgreementText(Context ctx) {
        return ctx.getString(R.string.agreement_text);
    }
}