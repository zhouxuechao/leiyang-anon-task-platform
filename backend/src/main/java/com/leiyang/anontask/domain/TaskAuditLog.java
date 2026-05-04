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
@Table(name = "task_audit_log")
@Getter
@Setter
public class TaskAuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "biz_type", nullable = false, length = 32)
  private String bizType;

  @Column(name = "biz_id", nullable = false)
  private Long bizId;

  @Column(name = "result", nullable = false, length = 32)
  private String result;

  @Column(name = "reason", length = 256)
  private String reason;

  @Column(name = "auditor_id")
  private Long auditorId;

  @Column(name = "auditor_type", nullable = false, length = 16)
  private String auditorType;

  @Column(name = "audit_time", nullable = false)
  private Instant auditTime;
}

