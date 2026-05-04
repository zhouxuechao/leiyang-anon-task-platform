package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
    @NotBlank(message = "targetType is required") String targetType,
    @NotNull(message = "targetId is required") Long targetId,
    @NotBlank(message = "reason is required") String reason
) {}

