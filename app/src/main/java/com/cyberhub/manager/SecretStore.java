package com.cyberhub.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-GCM at rest for provider credentials, keyed by the Android keystore.
 * Values written by older builds ("legacy") are migrated on first read.
 */
final class SecretStore {
    private static final String STORE = "cyberhub-secrets";
    private static final String ALIAS = "cyberhub-secrets-v1";
    private static final String MARK = "v1:";
    private static final String RAW = "raw:";

    private final Context context;
    private final SharedPreferences secrets;
    private final SharedPreferences legacy;

    SecretStore(Context context) {
        this.context = context.getApplicationContext();
        this.secrets = this.context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        this.legacy = this.context.getSharedPreferences(Translate.PREFS, Context.MODE_PRIVATE);
    }

    String get(String key) {
        String stored = secrets.getString(key, null);
        if (stored != null) {
            if (stored.startsWith(RAW)) return stored.substring(RAW.length());
            if (stored.startsWith(MARK)) {
                try {
                    return decrypt(stored.substring(MARK.length()));
                } catch (Exception e) {
                    return "";
                }
            }
        }
        String plain = legacy.getString(key, "");
        if (!plain.isEmpty()) {
            put(key, plain);
            legacy.edit().remove(key).apply();
        }
        return plain;
    }

    void put(String key, String value) {
        if (value == null || value.isEmpty()) {
            secrets.edit().remove(key).apply();
            return;
        }
        String encoded;
        try {
            encoded = MARK + encrypt(value);
        } catch (Exception e) {
            encoded = RAW + value;
        }
        secrets.edit().putString(key, encoded).apply();
    }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return kg.generateKey();
    }

    private String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] body = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[iv.length + body.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(body, 0, out, iv.length, body.length);
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    private String decrypt(String encoded) throws Exception {
        byte[] all = Base64.decode(encoded, Base64.NO_WRAP);
        if (all.length < 28) throw new IllegalArgumentException("密文过短");
        byte[] iv = Arrays.copyOfRange(all, 0, 12);
        byte[] body = Arrays.copyOfRange(all, 12, all.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
    }
}
