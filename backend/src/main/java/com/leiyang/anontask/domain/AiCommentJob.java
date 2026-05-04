package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_comment_job")
@Getter
@Setter
public class AiCommentJob extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "post_id", nullable = false)
  private PlazaPost post;

  @Column(name = "provider_code", nullable = false, length = 64)
  private String providerCode;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "next_retry_at", nullable = false)
  private Instant nextRetryAt;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "comment_id")
  private Long commentId;

  @Column(name = "processed_at")
  private Instant processedAt;
}
