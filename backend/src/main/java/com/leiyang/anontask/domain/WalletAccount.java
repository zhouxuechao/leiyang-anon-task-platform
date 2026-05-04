package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wallet_account")
@Getter
@Setter
public class WalletAccount extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserAccount user;

  @Column(name = "balance", nullable = false, precision = 12, scale = 2)
  private BigDecimal balance;

  @Column(name = "frozen_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal frozenAmount;

  @Column(name = "total_income", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalIncome;
}

