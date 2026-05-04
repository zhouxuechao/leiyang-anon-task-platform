package com.leiyang.anontask.dto.admin;

import java.math.BigDecimal;

public record TaskAdminDetail(
    String taskNo,
    String status,
    String title,
    String content,
    String locationText,
    BigDecimal amount,
    int totalSlots,
    int acceptedSlots,
    String deadlineAt,
    String proofRequirements,
    String rejectReason,
    String publisherOpenId,
    String createdAt
) {}

