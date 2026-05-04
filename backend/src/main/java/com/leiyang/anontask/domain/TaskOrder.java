package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.OrderStatus;
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
@Table(name = "task_order")
@Getter
@Setter
public class TaskOrder extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_no", nullable = false, unique = true, length = 32)
  private String orderNo;

  @ManyToOne
  @JoinColumn(name = "task_id", nullable = false)
  private TaskPublish task;

  @ManyToOne
  @JoinColumn(name = "accept_user_id", nullable = false)
  private UserAccount acceptUser;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status", nullable = false, length = 32)
  private OrderStatus orderStatus;

  @Column(name = "accept_time", nullable = false)
  private Instant acceptTime;

  @Column(name = "submit_time")
  private Instant submitTime;

  @Column(name = "audit_reason", length = 256)
  private String auditReason;

  @Column(name = "settled_time")
  private Instant settledTime;

  @Column(name = "settled_amount", precision = 12, scale = 2)
  private BigDecimal settledAmount;
}
