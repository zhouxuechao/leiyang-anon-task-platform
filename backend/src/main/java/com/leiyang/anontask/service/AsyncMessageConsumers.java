package com.leiyang.anontask.service;

import com.leiyang.anontask.config.AsyncMessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
public class AsyncMessageConsumers {
  private static final Logger log = LoggerFactory.getLogger(AsyncMessageConsumers.class);

  private final EmailCodeService emailCodeService;
  private final MessageService messageService;
  private final AiAutomationService aiAutomationService;
  private final AdminAuditService adminAuditService;

  public AsyncMessageConsumers(
      EmailCodeService emailCodeService,
      MessageService messageService,
      AiAutomationService aiAutomationService,
      AdminAuditService adminAuditService
  ) {
    this.emailCodeService = emailCodeService;
    this.messageService = messageService;
    this.aiAutomationService = aiAutomationService;
    this.adminAuditService = adminAuditService;
  }

  @RabbitListener(queues = AsyncMessagingConfig.EMAIL_QUEUE)
  public void handleEmail(EmailCodeService.EmailSendEvent event) {
    if (event == null) return;
    log.info("mq_consume_email email={}", maskEmail(event.email()));
    emailCodeService.sendMailNow(event.email(), event.code());
  }

  @RabbitListener(queues = AsyncMessagingConfig.MESSAGE_QUEUE)
  public void handleMessage(MessageService.UserMessageEvent event) {
    if (event == null) return;
    log.info("mq_consume_message userId={} type={}", event.userId(), event.type());
    messageService.saveDirect(event.userId(), event.type(), event.title(), event.content());
  }

  @RabbitListener(queues = AsyncMessagingConfig.AI_COMMENT_QUEUE)
  public void handleAiComment(AiAutomationService.AiCommentConsumeEvent event) {
    log.info("mq_consume_ai_comment reason={}", event == null ? "" : event.reason());
    aiAutomationService.triggerCommentConsumeOnce();
  }

  @RabbitListener(queues = AsyncMessagingConfig.TASK_SETTLE_QUEUE)
  public void handleTaskSettle(AdminAuditService.TaskSettleEvent event) {
    if (event == null || event.taskNo() == null || event.taskNo().isBlank()) return;
    log.info("mq_consume_task_settle taskNo={}", event.taskNo());
    adminAuditService.autoSettleExpiredTask(event.taskNo());
  }

  private static String maskEmail(String email) {
    if (email == null || email.isBlank()) return "";
    int at = email.indexOf('@');
    if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
    return email.charAt(0) + "***" + email.substring(at);
  }
}
