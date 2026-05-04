package com.leiyang.anontask.dto.admin;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailResponse(
    String orderNo,
    String orderStatus,
    String auditReason,
    String acceptUserOpenId,
    String submitTime,
    String taskNo,
    String taskTitle,
    BigDecimal taskAmount,
    List<ProofItem> proofs
) {
  public record ProofItem(String type, String url, String remark, String createdAt) {}
}

