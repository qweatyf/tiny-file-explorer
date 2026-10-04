package com.example.myempty.activity2;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class ChatCrypto {

    private static final String FIXED_SALT = "MyEmptyChat_Salt_v1_DoNotChange";
    private static final int PBKDF2_ITER = 20000;
    private static final int KEY_BITS = 256;
    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = 16;
    private static final String AES_ALGO = "AES/GCM/NoPadding";

    private static final SecureRandom RNG = new SecureRandom();

    public static byte[] deriveKey(String passphrase, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
            passphrase.toCharArray(), salt, PBKDF2_ITER, KEY_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    public static byte[] sessionKeyFromPass(String passphrase) throws Exception {
        byte[] salt = FIXED_SALT.getBytes("UTF-8");
        return deriveKey(passphrase, salt);
    }

    public static String passHash(String passphrase) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(FIXED_SALT.getBytes("UTF-8"));
            md.update(passphrase.getBytes("UTF-8"));
            byte[] h = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b & 0xFF));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String encrypt(String plain, byte[] key) {
        try {
            byte[] data = plain.getBytes("UTF-8");
            byte[] nonce = new byte[NONCE_LEN];
            RNG.nextBytes(nonce);

            Cipher c = Cipher.getInstance(AES_ALGO);
            c.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ct = c.doFinal(data);

            byte[] out = new byte[NONCE_LEN + ct.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_LEN);
            System.arraycopy(ct, 0, out, NONCE_LEN, ct.length);
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decrypt(String b64, byte[] key) {
        try {
            byte[] all = Base64.decode(b64, Base64.DEFAULT);
            if (all.length < NONCE_LEN + TAG_BYTES) return null;

            byte[] nonce = new byte[NONCE_LEN];
            System.arraycopy(all, 0, nonce, 0, NONCE_LEN);
            byte[] ct = new byte[all.length - NONCE_LEN];
            System.arraycopy(all, NONCE_LEN, ct, 0, ct.length);

            Cipher c = Cipher.getInstance(AES_ALGO);
            c.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, nonce));
            byte[] pt = c.doFinal(ct);
            return new String(pt, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] encryptBytes(byte[] plain, byte[] key) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            RNG.nextBytes(nonce);

            Cipher c = Cipher.getInstance(AES_ALGO);
            c.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ct = c.doFinal(plain);

            byte[] out = new byte[NONCE_LEN + ct.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_LEN);
            System.arraycopy(ct, 0, out, NONCE_LEN, ct.length);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] decryptBytes(byte[] enc, byte[] key) {
        try {
            if (enc == null || enc.length < NONCE_LEN + TAG_BYTES) return null;
            byte[] nonce = new byte[NONCE_LEN];
            System.arraycopy(enc, 0, nonce, 0, NONCE_LEN);
            byte[] ct = new byte[enc.length - NONCE_LEN];
            System.arraycopy(enc, NONCE_LEN, ct, 0, ct.length);

            Cipher c = Cipher.getInstance(AES_ALGO);
            c.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, nonce));
            return c.doFinal(ct);
        } catch (Exception e) {
            return null;
        }
    }
}