package com.example.myempty.activity2;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA1";
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int PBKDF2_ITER = 100000;
    private static final int KEY_BITS = 256;
    private static final int SALT_LEN = 16;
    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = 16;

    private static final SecureRandom RNG = new SecureRandom();

    static byte[] derive(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITER, KEY_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGO);
        return skf.generateSecret(spec).getEncoded();
    }

    public static byte[] aesEncrypt(byte[] data, String password) throws Exception {
        byte[] salt = new byte[SALT_LEN];
        RNG.nextBytes(salt);

        byte[] key = derive(password, salt);

        byte[] nonce = new byte[NONCE_LEN];
        RNG.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BITS, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), gcmSpec);

        byte[] ct = cipher.doFinal(data);

        byte[] out = new byte[SALT_LEN + NONCE_LEN + ct.length];
        System.arraycopy(salt, 0, out, 0, SALT_LEN);
        System.arraycopy(nonce, 0, out, SALT_LEN, NONCE_LEN);
        System.arraycopy(ct, 0, out, SALT_LEN + NONCE_LEN, ct.length);
        return out;
    }

    public static byte[] aesDecrypt(byte[] data, String password) throws Exception {
        if (data.length < SALT_LEN + NONCE_LEN + TAG_BYTES) {
            throw new Exception("太短了吧");
        }

        byte[] salt = new byte[SALT_LEN];
        System.arraycopy(data, 0, salt, 0, SALT_LEN);

        byte[] nonce = new byte[NONCE_LEN];
        System.arraycopy(data, SALT_LEN, nonce, 0, NONCE_LEN);

        byte[] ct = new byte[data.length - SALT_LEN - NONCE_LEN];
        System.arraycopy(data, SALT_LEN + NONCE_LEN, ct, 0, ct.length);

        byte[] key = derive(password, salt);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BITS, nonce);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), gcmSpec);

        return cipher.doFinal(ct);
    }

    public static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}