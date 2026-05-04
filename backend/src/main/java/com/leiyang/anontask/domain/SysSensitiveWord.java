package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.GeneralStatus;
import com.leiyang.anontask.domain.enums.SensitiveWordAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sys_sensitive_word")
@Getter
@Setter
public class SysSensitiveWord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "word", nullable = false, unique = true, length = 64)
  private String word;

  @Column(name = "level", nullable = false)
  private int level;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 32)
  private SensitiveWordAction actionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private GeneralStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

