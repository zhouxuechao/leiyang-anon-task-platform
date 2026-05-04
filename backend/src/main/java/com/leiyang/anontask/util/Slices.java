package com.leiyang.anontask.util;

import com.leiyang.anontask.dto.mp.SliceResponse;
import java.util.List;

public final class Slices {
  private Slices() {}

  public static int safePage(int page) {
    return Math.max(1, page);
  }

  public static int safeSize(int size, int max) {
    return Math.min(Math.max(size, 1), max);
  }

  public static <T> SliceResponse<T> of(List<T> rows, int page, int size) {
    boolean hasMore = rows.size() > size;
    return new SliceResponse<>(hasMore ? rows.subList(0, size) : rows, page, size, hasMore);
  }
}
