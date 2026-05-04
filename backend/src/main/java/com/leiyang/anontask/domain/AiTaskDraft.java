package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_task_draft")
@Getter
@Setter
public class AiTaskDraft extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "provider_code", nullable = false, length = 64)
  private String providerCode;

  @Column(name = "title", nullable = false, length = 128)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "category", length = 32)
  private String category;

  @Column(name = "location_text", length = 256)
  private String locationText;

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "total_slots", nullable = false)
  private int totalSlots;

  @Column(name = "deadline_at", nullable = false)
  private Instant deadlineAt;

  @Column(name = "proof_requirements", length = 256)
  private String proofRequirements;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "published_task_no", length = 32)
  private String publishedTaskNo;

  @Column(name = "raw_response", columnDefinition = "text")
  private String rawResponse;
}
