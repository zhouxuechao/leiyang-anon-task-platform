package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recharge_order")
@Getter
@Setter
public class RechargeOrder extends BaseEntity {

  // status 常量
  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_SUCCESS = "SUCCESS";
  public static final String STATUS_CLOSED  = "CLOSED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "out_trade_no", nullable = false, unique = true, length = 64)
  private String outTradeNo;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  /** 金额，单位：分 */
  @Column(name = "amount_fen", nullable = false)
  private int amountFen;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "wx_transaction_id", length = 64)
  private String wxTransactionId;
}
