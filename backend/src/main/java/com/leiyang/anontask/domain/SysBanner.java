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
@Table(name = "sys_banner")
@Getter
@Setter
public class SysBanner {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "image_url", nullable = false, length = 1024)
  private String imageUrl;

  @Column(name = "link_url", length = 1024)
  private String linkUrl;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "sort_no", nullable = false)
  private int sortNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

