package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneLoginRequest(
    @NotBlank(message = "mobile is required")
    @Pattern(regexp = "^1\\d{10}$", message = "mobile format invalid")
    String mobile,
    @NotBlank(message = "code is required") String code,
    String nickname,
    String avatar
) {}
