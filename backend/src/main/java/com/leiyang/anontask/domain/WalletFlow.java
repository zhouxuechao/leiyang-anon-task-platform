package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.FlowStatus;
import com.leiyang.anontask.domain.enums.WalletFlowType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "wallet_flow")
@Getter
@Setter
public class WalletFlow {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Enumerated(EnumType.STRING)
  @Column(name = "flow_type", nullable = false, length = 32)
  private WalletFlowType flowType;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "biz_no", length = 64)
  private String bizNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private FlowStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

