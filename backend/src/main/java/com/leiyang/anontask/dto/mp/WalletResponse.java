package com.leiyang.anontask.dto.mp;

import java.math.BigDecimal;

public record WalletResponse(
    BigDecimal balance,
    BigDecimal frozenAmount,
    BigDecimal totalIncome,
    String withdrawQrCodeUrl
) {}
