package com.leiyang.anontask.job;

import com.leiyang.anontask.service.AiAutomationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiAutomationJob {
  private final AiAutomationService aiAutomationService;

  public AiAutomationJob(AiAutomationService aiAutomationService) {
    this.aiAutomationService = aiAutomationService;
  }

  @Scheduled(fixedDelayString = "PT30M")
  public void autoPublishAiTask() {
    aiAutomationService.autoPublishAiTaskOnce();
  }

  @Scheduled(fixedDelayString = "PT15S")
  public void consumePendingAiCommentJobs() {
    aiAutomationService.triggerCommentConsumeOnce();
  }
}
