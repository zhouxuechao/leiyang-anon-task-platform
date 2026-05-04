package com.leiyang.anontask.dto.mp;

import java.math.BigDecimal;
import java.time.Instant;

public record TaskListItem(
    String taskNo,
    String title,
    String category,
    BigDecimal amount,
    String locationText,
    int totalSlots,
    int acceptedSlots,
    Instant createdAt,
    Instant deadlineAt,
    String status,
    String myOrderNo,
    String myOrderStatus
) {}
