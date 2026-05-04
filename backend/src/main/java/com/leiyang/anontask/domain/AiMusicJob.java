package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_music_job")
@Getter
@Setter
public class AiMusicJob extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "prompt", nullable = false, columnDefinition = "text")
  private String prompt;

  @Column(name = "style", length = 1000)
  private String style;

  @Column(name = "custom_mode", nullable = false)
  private boolean customMode;

  @Column(name = "instrumental", nullable = false)
  private boolean instrumental;

  @Column(name = "lang", length = 32)
  private String lang;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "suno_task_id", length = 128)
  private String sunoTaskId;

  @Column(name = "suno_audio_id", length = 128)
  private String sunoAudioId;

  @Column(name = "audio_url", length = 2048)
  private String audioUrl;

  @Column(name = "video_url", length = 2048)
  private String videoUrl;

  @Column(name = "image_url", length = 2048)
  private String imageUrl;

  @Column(name = "duration", length = 32)
  private String duration;

  @Column(name = "lyrics", columnDefinition = "text")
  private String lyrics;

  @Column(name = "raw_response", columnDefinition = "text")
  private String rawResponse;

  @Column(name = "error_message", length = 1024)
  private String errorMessage;

  @Column(name = "published", nullable = false)
  private boolean published;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "plaza_post_id")
  private Long plazaPostId;

  @Column(name = "rating_total", nullable = false)
  private int ratingTotal;

  @Column(name = "rating_count", nullable = false)
  private int ratingCount;

  @Column(name = "tip_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal tipTotal = BigDecimal.ZERO;
}
