package com.leiyang.anontask.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.aopalliance.aop.Advice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
public class AsyncMessagingConfig {
  public static final String EXCHANGE = "diaogualai.events";
  public static final String AI_COMMENT_QUEUE = "diaogualai.ai-comment.consume";
  public static final String EMAIL_QUEUE = "diaogualai.email.send";
  public static final String MESSAGE_QUEUE = "diaogualai.message.create";
  public static final String TASK_SETTLE_QUEUE = "diaogualai.task.settle";
  public static final String AI_COMMENT_ROUTING_KEY = "ai-comment.consume";
  public static final String EMAIL_ROUTING_KEY = "email.send";
  public static final String MESSAGE_ROUTING_KEY = "message.create";
  public static final String TASK_SETTLE_ROUTING_KEY = "task.settle";

  @Bean
  DirectExchange diaogualaiExchange() {
    return new DirectExchange(EXCHANGE, true, false);
  }

  @Bean
  Queue aiCommentQueue() {
    return new Queue(AI_COMMENT_QUEUE, true);
  }

  @Bean
  Queue emailQueue() {
    return new Queue(EMAIL_QUEUE, true);
  }

  @Bean
  Queue messageQueue() {
    return new Queue(MESSAGE_QUEUE, true);
  }

  @Bean
  Queue taskSettleQueue() {
    return new Queue(TASK_SETTLE_QUEUE, true);
  }

  @Bean
  Binding aiCommentBinding(Queue aiCommentQueue, DirectExchange diaogualaiExchange) {
    return BindingBuilder.bind(aiCommentQueue).to(diaogualaiExchange).with(AI_COMMENT_ROUTING_KEY);
  }

  @Bean
  Binding emailBinding(Queue emailQueue, DirectExchange diaogualaiExchange) {
    return BindingBuilder.bind(emailQueue).to(diaogualaiExchange).with(EMAIL_ROUTING_KEY);
  }

  @Bean
  Binding messageBinding(Queue messageQueue, DirectExchange diaogualaiExchange) {
    return BindingBuilder.bind(messageQueue).to(diaogualaiExchange).with(MESSAGE_ROUTING_KEY);
  }

  @Bean
  Binding taskSettleBinding(Queue taskSettleQueue, DirectExchange diaogualaiExchange) {
    return BindingBuilder.bind(taskSettleQueue).to(diaogualaiExchange).with(TASK_SETTLE_ROUTING_KEY);
  }

  @Bean
  MessageConverter rabbitMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  Advice rabbitRetryInterceptor(
      MessageRecoverer mqFallbackRecoverer,
      @Value("${app.mq.retry.max-attempts:3}") int maxAttempts,
      @Value("${app.mq.retry.initial-interval-ms:1000}") long initialIntervalMs,
      @Value("${app.mq.retry.multiplier:2.0}") double multiplier,
      @Value("${app.mq.retry.max-interval-ms:10000}") long maxIntervalMs
  ) {
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(Math.max(1, maxAttempts))
        .backOffOptions(
            Math.max(100L, initialIntervalMs),
            Math.max(1.0, multiplier),
            Math.max(initialIntervalMs, maxIntervalMs)
        )
        .recoverer(mqFallbackRecoverer)
        .build();
  }

  @Bean
  SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      MessageConverter rabbitMessageConverter,
      Advice rabbitRetryInterceptor
  ) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(rabbitMessageConverter);
    factory.setAdviceChain(rabbitRetryInterceptor);
    factory.setDefaultRequeueRejected(false);
    return factory;
  }
}
