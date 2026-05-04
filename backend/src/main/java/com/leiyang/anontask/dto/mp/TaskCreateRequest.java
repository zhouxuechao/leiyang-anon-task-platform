package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record TaskCreateRequest(
    @NotBlank(message = "title is required") String title,
    @NotBlank(message = "content is required") String content,
    String category,
    String locationText,
    @NotNull(message = "amount is required") BigDecimal amount,
    @Min(value = 1, message = "totalSlots must be >= 1") int totalSlots,
    @NotNull(message = "deadlineAt is required") Instant deadlineAt,
    String proofRequirements
) {}
