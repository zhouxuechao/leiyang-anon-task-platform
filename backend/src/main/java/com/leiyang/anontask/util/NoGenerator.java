package com.leiyang.anontask.util;

import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class NoGenerator {
  private static final SecureRandom RND = new SecureRandom();
  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyMMddHHmmss");

  private NoGenerator() {}

  public static String gen(String prefix) {
    String ts = ZonedDateTime.now(ZoneOffset.UTC).format(TS);
    int n = RND.nextInt(1000);
    return prefix + ts + String.format("%03d", n);
  }
}

