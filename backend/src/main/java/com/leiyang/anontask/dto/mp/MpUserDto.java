package com.leiyang.anontask.dto.mp;

public record MpUserDto(
    long id,
    String openId,
    String nickname,
    String avatar,
    String gender,
    String signature,
    String status,
    int creditScore
) {}
