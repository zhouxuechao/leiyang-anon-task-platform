package com.leiyang.anontask.service;

import com.leiyang.anontask.config.AsyncMessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageQueueService {
  private static final Logger log = LoggerFactory.getLogger(MessageQueueService.class);

  private final ObjectProvider<RabbitTemplate> rabbitTemplate;
  private final boolean enabled;

  public MessageQueueService(
      ObjectProvider<RabbitTemplate> rabbitTemplate,
      @Value("${app.mq.enabled:false}") boolean enabled
  ) {
    this.rabbitTemplate = rabbitTemplate;
    this.enabled = enabled;
  }

  public boolean publish(String routingKey, Object payload) {
    if (!enabled) return false;
    try {
      RabbitTemplate template = rabbitTemplate.getIfAvailable();
      if (template == null) {
        log.warn("mq_publish_skipped routingKey={} reason=rabbitTemplateMissing", routingKey);
        return false;
      }
      template.convertAndSend(AsyncMessagingConfig.EXCHANGE, routingKey, payload);
      log.debug("mq_publish_success routingKey={} payloadType={}", routingKey, payload == null ? "" : payload.getClass().getSimpleName());
      return true;
    } catch (Exception e) {
      log.warn("mq_publish_failed routingKey={} payloadType={} error={}", routingKey, payload == null ? "" : payload.getClass().getSimpleName(), e.toString());
      return false;
    }
  }
}
