package com.leiyang.anontask.security;

public record AuthPrincipal(
    long id,
    AuthRole role
) {}

