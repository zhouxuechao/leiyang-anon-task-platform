package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;

public record MpLoginRequest(
    @NotBlank(message = "openId is required") String openId,
    String nickname,
    String avatar
) {}

