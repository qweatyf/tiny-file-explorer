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

    public static String getAgreementText() {
        return "使用前请阅读以下条款：\n\n" +
            "1. 本项目为开源工具，仅供学习与个人使用。\n" +
            "2. 禁止将本项目用于任何违法违规用途。\n" +
            "3. 部分功能涉及系统权限，请自行承担使用风险。\n" +
            "4. 本项目不收集、不上传任何用户数据。\n" +
            "5. 使用本项目即表示你已阅读并同意以上条款。\n\n" +
            "点击\"同意\"代表你已阅读并接受。";
    }
}