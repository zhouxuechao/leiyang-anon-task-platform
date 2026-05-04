package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;

public record SubmitProofItem(
    @NotBlank(message = "type is required") String type,
    @NotBlank(message = "url is required") String url,
    String remark
) {}

