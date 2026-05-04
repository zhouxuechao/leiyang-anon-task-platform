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
@Table(name = "plaza_post_image")
@Getter
@Setter
public class PlazaPostImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "post_id", nullable = false)
  private PlazaPost post;

  @Column(name = "image_url", nullable = false, length = 1024)
  private String imageUrl;

  @Column(name = "sort_no", nullable = false)
  private int sortNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

