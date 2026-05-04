package com.leiyang.anontask.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AuditRequest(
    @NotBlank(message = "result is required") String result,
    String reason
) {}

