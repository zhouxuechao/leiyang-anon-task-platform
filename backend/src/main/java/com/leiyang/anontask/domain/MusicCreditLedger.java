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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "music_credit_ledger")
@Getter
@Setter
public class MusicCreditLedger {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "change_amount", nullable = false)
  private int changeAmount;

  @Column(name = "biz_type", nullable = false, length = 32)
  private String bizType;

  @Column(name = "biz_no", length = 64)
  private String bizNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
