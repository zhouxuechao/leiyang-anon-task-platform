package com.leiyang.anontask.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {
  private static final SecureRandom RND = new SecureRandom();

  private final SecretKey key;

  public CryptoService(@Value("${APP_CRYPTO_KEY:}") String base64Key) {
    this.key = parseKey(base64Key);
  }

  private static SecretKey parseKey(String base64Key) {
    if (base64Key == null || base64Key.isBlank()) {
      return null;
    }
    byte[] raw = Base64.getDecoder().decode(base64Key.trim());
    if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
      throw new IllegalArgumentException("APP_CRYPTO_KEY must be base64 for 16/24/32 bytes AES key");
    }
    return new SecretKeySpec(raw, "AES");
  }

  public String encryptOrFallback(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    if (key == null) {
      return "b64:" + Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }
    try {
      byte[] iv = new byte[12];
      RND.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] out = new byte[iv.length + ct.length];
      System.arraycopy(iv, 0, out, 0, iv.length);
      System.arraycopy(ct, 0, out, iv.length, ct.length);
      return "gcm:" + Base64.getEncoder().encodeToString(out);
    } catch (Exception e) {
      return "b64:" + Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }
  }
}

