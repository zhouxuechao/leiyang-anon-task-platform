package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plaza_ai_provider")
@Getter
@Setter
public class PlazaAiProvider extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 64)
  private String name;

  @Column(name = "abbr", nullable = false, length = 16)
  private String abbr;

  @Column(name = "logo_text", length = 16)
  private String logoText;

  @Column(name = "logo_url", length = 512)
  private String logoUrl;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "sort_no", nullable = false)
  private int sortNo;

  @Column(name = "base_url", length = 255)
  private String baseUrl;

  @Column(name = "model", length = 128)
  private String model;

  @Column(name = "api_key", length = 512)
  private String apiKey;

  @Column(name = "timeout_ms")
  private Integer timeoutMs;

  @Column(name = "temperature")
  private Double temperature;
}
