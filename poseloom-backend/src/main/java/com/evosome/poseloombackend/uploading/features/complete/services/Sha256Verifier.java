package com.evosome.poseloombackend.uploading.features.complete.services;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Потоковый SHA-256 собранного объекта (Tasks T3.6): канон целостности —
 * SHA-256, а не multipart ETag (тот составной и пригоден только для
 * CompleteMultipartUpload). Чтение ограниченным буфером, файл целиком в память
 * не загружается; hex — нижний регистр, как в доменной конвенции.
 */
public final class Sha256Verifier {

    private static final int BUFFER_SIZE = 8192;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Sha256Verifier() {
    }

    /**
     * Считает SHA-256 всего потока.
     *
     * @param source поток объекта; не закрывается — close на вызывающем
     * @return 64 hex-символа в нижнем регистре
     * @throws IOException при сбое чтения из хранилища
     */
    public static String hexDigest(InputStream source) throws IOException {
        MessageDigest digest = newSha256();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = source.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return toHex(digest.digest());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available in the JRE", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            hex[i * 2] = HEX[value >>> 4];
            hex[i * 2 + 1] = HEX[value & 0x0F];
        }
        return new String(hex);
    }
}
