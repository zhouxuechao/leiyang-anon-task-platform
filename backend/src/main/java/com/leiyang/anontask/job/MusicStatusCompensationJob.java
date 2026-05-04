package com.leiyang.anontask.job;

import com.leiyang.anontask.service.AiMusicService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MusicStatusCompensationJob {
  private final AiMusicService aiMusicService;

  public MusicStatusCompensationJob(AiMusicService aiMusicService) {
    this.aiMusicService = aiMusicService;
  }

  @Scheduled(fixedDelayString = "PT30S")
  public void enqueuePendingMusicRefreshes() {
    aiMusicService.enqueuePendingRefreshes();
  }
}
