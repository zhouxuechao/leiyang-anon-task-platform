package com.leiyang.anontask.dto.mp;

import java.util.List;

public record SubmitOrderRequest(
    List<SubmitProofItem> proofs
) {}
