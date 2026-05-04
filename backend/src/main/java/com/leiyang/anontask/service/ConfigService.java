package com.leiyang.anontask.service;

import com.leiyang.anontask.repo.SysConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {
  private final SysConfigRepository repo;

  public ConfigService(SysConfigRepository repo) {
    this.repo = repo;
  }

  public int getInt(String key, int defaultValue) {
    return repo.findByCfgKey(key)
        .map(c -> {
          try {
            return Integer.parseInt(c.getCfgValue().trim());
          } catch (Exception e) {
            return defaultValue;
          }
        })
        .orElse(defaultValue);
  }

  public double getDouble(String key, double defaultValue) {
    return repo.findByCfgKey(key)
        .map(c -> {
          try {
            return Double.parseDouble(c.getCfgValue().trim());
          } catch (Exception e) {
            return defaultValue;
          }
        })
        .orElse(defaultValue);
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return repo.findByCfgKey(key)
        .map(c -> {
          String v = c.getCfgValue() == null ? "" : c.getCfgValue().trim().toLowerCase();
          if ("true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v)) return true;
          if ("false".equals(v) || "0".equals(v) || "no".equals(v) || "off".equals(v)) return false;
          return defaultValue;
        })
        .orElse(defaultValue);
  }

  public String getString(String key, String defaultValue) {
    return repo.findByCfgKey(key)
        .map(c -> c.getCfgValue() == null ? "" : c.getCfgValue().trim())
        .filter(v -> !v.isEmpty())
        .orElse(defaultValue);
  }
}
