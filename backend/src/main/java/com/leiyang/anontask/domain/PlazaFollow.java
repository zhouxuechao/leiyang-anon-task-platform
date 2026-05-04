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
@Table(name = "plaza_follow")
@Getter
@Setter
public class PlazaFollow {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @ManyToOne
  @JoinColumn(name = "target_user_id", nullable = false)
  private UserAccount targetUser;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

