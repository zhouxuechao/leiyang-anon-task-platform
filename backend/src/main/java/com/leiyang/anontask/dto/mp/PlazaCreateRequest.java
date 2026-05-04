package com.leiyang.anontask.dto.mp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlazaCreateRequest(
    @Size(max = 500, message = "content too long") String content,
    @NotBlank(message = "gender is required") String gender,
    String category,
    @Size(max = 9, message = "max 9 images") List<String> images
) {}
