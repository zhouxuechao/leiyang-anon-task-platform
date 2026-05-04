package com.leiyang.anontask.dto.mp;

import java.time.Instant;

public record TaskCommentItem(
    long id,
    String displayName,
    String content,
    Instant createdAt
) {}

