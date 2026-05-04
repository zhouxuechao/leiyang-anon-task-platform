package com.leiyang.anontask.dto.mp;

import java.math.BigDecimal;
import java.time.Instant;

public record TaskDetailResponse(
    String taskNo,
    String title,
    String content,
    String category,
    String locationText,
    BigDecimal amount,
    int totalSlots,
    int acceptedSlots,
    Instant createdAt,
    Instant deadlineAt,
    String proofRequirements,
    String status
) {}
