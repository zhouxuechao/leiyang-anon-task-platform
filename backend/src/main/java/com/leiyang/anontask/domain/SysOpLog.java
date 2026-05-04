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
@Table(name = "sys_op_log")
@Getter
@Setter
public class SysOpLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "actor_type", nullable = false, length = 16)
  private String actorType;

  @Column(name = "actor_id", nullable = false)
  private long actorId;

  @Column(name = "method", nullable = false, length = 16)
  private String method;

  @Column(name = "path", nullable = false, length = 256)
  private String path;

  @Column(name = "ip", length = 64)
  private String ip;

  @Column(name = "user_agent", length = 256)
  private String userAgent;

  @Column(name = "ok_flag", nullable = false)
  private boolean okFlag;

  @Column(name = "error_msg", length = 256)
  private String errorMsg;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

