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
@Table(name = "sys_config")
@Getter
@Setter
public class SysConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cfg_key", nullable = false, unique = true, length = 64)
  private String cfgKey;

  @Column(name = "cfg_value", nullable = false, length = 512)
  private String cfgValue;

  @Column(name = "remark", length = 256)
  private String remark;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}

