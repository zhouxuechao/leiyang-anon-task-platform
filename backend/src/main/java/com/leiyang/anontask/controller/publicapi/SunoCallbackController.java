package com.leiyang.anontask.controller.publicapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.service.AiMusicService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/suno")
public class SunoCallbackController {
  private final AiMusicService musicService;

  public SunoCallbackController(AiMusicService musicService) {
    this.musicService = musicService;
  }

  @PostMapping("/callback")
  public ApiResult<Void> callback(@RequestBody JsonNode body) {
    musicService.handleCallback(body);
    return ApiResult.ok(null);
  }
}
