package com.leiyang.anontask.dto.mp;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderListItem(
    String orderNo,
    String taskNo,
    String taskTitle,
    BigDecimal amount,
    String orderStatus,
    Instant acceptTime,
    Instant submitTime,
    String auditReason
) {}

