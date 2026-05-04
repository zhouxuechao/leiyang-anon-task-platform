package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;

public record RealnameRequest(
    @NotBlank(message = "realName is required") String realName,
    @NotBlank(message = "idNo is required") String idNo,
    @NotBlank(message = "mobile is required") String mobile,
    String payAccount
) {}

