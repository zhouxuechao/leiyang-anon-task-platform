package com.leiyang.anontask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leiyang.anontask.config.AsyncMessagingConfig;
import com.leiyang.anontask.domain.MqFailedMessage;
import com.leiyang.anontask.repo.MqFailedMessageRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
public class MqFallbackRecoverer implements MessageRecoverer {
  private static final Logger log = LoggerFactory.getLogger(MqFallbackRecoverer.class);

  private final ObjectMapper mapper;
  private final EmailCodeService emailCodeService;
  private final MessageService messageService;
  private final AiAutomationService aiAutomationService;
  private final AdminAuditService adminAuditService;
  private final MqFailedMessageRepository failedRepo;

  public MqFallbackRecoverer(
      ObjectMapper mapper,
      EmailCodeService emailCodeService,
      MessageService messageService,
      AiAutomationService aiAutomationService,
      AdminAuditService adminAuditService,
      MqFailedMessageRepository failedRepo
  ) {
    this.mapper = mapper;
    this.emailCodeService = emailCodeService;
    this.messageService = messageService;
    this.aiAutomationService = aiAutomationService;
    this.adminAuditService = adminAuditService;
    this.failedRepo = failedRepo;
  }

  @Override
  public void recover(Message message, Throwable cause) {
    String queue = message.getMessageProperties().getConsumerQueue();
    try {
      log.warn("mq_retry_exhausted queue={} error={}", queue, cause == null ? "" : cause.toString());
      if (AsyncMessagingConfig.EMAIL_QUEUE.equals(queue)) {
        var event = read(message, EmailCodeService.EmailSendEvent.class);
        emailCodeService.sendMailNow(event.email(), event.code());
      } else if (AsyncMessagingConfig.MESSAGE_QUEUE.equals(queue)) {
        var event = read(message, MessageService.UserMessageEvent.class);
        messageService.saveDirect(event.userId(), event.type(), event.title(), event.content());
      } else if (AsyncMessagingConfig.AI_COMMENT_QUEUE.equals(queue)) {
        aiAutomationService.triggerCommentConsumeOnce();
      } else if (AsyncMessagingConfig.TASK_SETTLE_QUEUE.equals(queue)) {
        var event = read(message, AdminAuditService.TaskSettleEvent.class);
        adminAuditService.autoSettleExpiredTask(event.taskNo());
      } else {
        log.error("mq_fallback_unknown_queue queue={} bodySize={}", queue, message.getBody() == null ? 0 : message.getBody().length);
        return;
      }
      log.warn("mq_fallback_success queue={}", queue);
    } catch (Exception e) {
      log.error("mq_fallback_failed queue={} originalError={} fallbackError={}", queue, cause == null ? "" : cause.toString(), e.toString(), e);
      saveFailed(message, cause, e);
    }
  }

  private <T> T read(Message message, Class<T> type) throws Exception {
    return mapper.readValue(message.getBody(), type);
  }

  private void saveFailed(Message message, Throwable original, Exception fallback) {
    try {
      MqFailedMessage failed = new MqFailedMessage();
      failed.setQueueName(message.getMessageProperties().getConsumerQueue());
      failed.setRoutingKey(message.getMessageProperties().getReceivedRoutingKey());
      failed.setPayload(cut(new String(message.getBody(), StandardCharsets.UTF_8), 12000));
      failed.setErrorMsg(cut("original=" + (original == null ? "" : original.toString()) + "; fallback=" + fallback, 1024));
      failed.setStatus("FAILED");
      failed.setCreatedAt(Instant.now());
      failedRepo.save(failed);
    } catch (Exception persistError) {
      log.error("mq_failed_message_persist_failed error={}", persistError.toString(), persistError);
    }
  }

  private static String cut(String value, int max) {
    String v = value == null ? "" : value;
    return v.length() <= max ? v : v.substring(0, max);
  }
}
