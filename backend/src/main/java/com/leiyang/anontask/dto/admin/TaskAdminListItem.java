package com.leiyang.anontask.dto.admin;

import java.math.BigDecimal;

public record TaskAdminListItem(
    String taskNo,
    String status,
    String title,
    BigDecimal amount,
    int totalSlots,
    int acceptedSlots,
    String deadlineAt,
    String publisherOpenId,
    String createdAt
) {}

