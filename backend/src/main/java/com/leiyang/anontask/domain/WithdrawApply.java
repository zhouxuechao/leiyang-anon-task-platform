package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.WithdrawStatus;
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
@Table(name = "withdraw_apply")
@Getter
@Setter
public class WithdrawApply extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "apply_no", nullable = false, unique = true, length = 32)
  private String applyNo;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "channel", nullable = false, length = 32)
  private String channel;

  @Column(name = "qr_code_url", length = 4096)
  private String qrCodeUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "audit_status", nullable = false, length = 32)
  private WithdrawStatus auditStatus;

  @Column(name = "audit_reason", length = 256)
  private String auditReason;

  @Column(name = "paid_proof_url", length = 4096)
  private String paidProofUrl;

  @Column(name = "pay_remark", length = 256)
  private String payRemark;

  @Column(name = "paid_at")
  private Instant paidAt;
}
