package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mq_failed_message")
@Getter
@Setter
public class MqFailedMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "queue_name", nullable = false, length = 128)
  private String queueName;

  @Column(name = "routing_key", length = 128)
  private String routingKey;

  @Column(name = "payload", columnDefinition = "text")
  private String payload;

  @Column(name = "error_msg", length = 1024)
  private String errorMsg;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
