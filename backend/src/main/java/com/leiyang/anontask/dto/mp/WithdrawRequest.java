package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WithdrawRequest(
    @NotNull(message = "amount is required") BigDecimal amount,
    @NotBlank(message = "channel is required") String channel,
    String qrCodeUrl
) {}
