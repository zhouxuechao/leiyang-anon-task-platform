package com.leiyang.anontask.dto.mp;

import java.time.Instant;
import java.util.List;

public record TaskSubmissionItem(
    String orderNo,
    String displayName,
    String avatar,
    String orderStatus,
    Instant submitTime,
    String settledAmount,
    long likeCount,
    boolean likedByMe,
    List<ProofItem> proofs
) {
  public record ProofItem(String type, String url, String remark) {}
}
