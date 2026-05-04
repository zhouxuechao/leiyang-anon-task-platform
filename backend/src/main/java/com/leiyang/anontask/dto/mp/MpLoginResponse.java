package com.leiyang.anontask.dto.mp;

public record MpLoginResponse(
    String token,
    MpUserDto user
) {}

