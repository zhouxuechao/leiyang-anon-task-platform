package com.leiyang.anontask.controller.common;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common")
public class CommonUploadController {
  private final UploadService uploadService;

  public CommonUploadController(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  public record UploadResp(String url) {}

  @PostMapping("/upload")
  public ApiResult<UploadResp> upload(@RequestParam("file") MultipartFile file) {
    return ApiResult.ok(new UploadResp(uploadService.save(file)));
  }
}

