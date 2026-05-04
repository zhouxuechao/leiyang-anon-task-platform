package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;

public record WxLoginRequest(
    @NotBlank(message = "code is required") String code,
    String phoneCode,
    String nickname,
    String avatar
) {}
