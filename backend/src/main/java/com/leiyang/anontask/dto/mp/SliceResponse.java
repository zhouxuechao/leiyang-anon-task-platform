package com.leiyang.anontask.dto.mp;

import java.util.List;

public record SliceResponse<T>(
    List<T> items,
    int page,
    int size,
    boolean hasMore
) {}
