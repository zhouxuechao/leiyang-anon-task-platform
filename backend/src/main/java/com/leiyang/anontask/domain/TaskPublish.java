package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.TaskStatus;
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
@Table(name = "task_publish")
@Getter
@Setter
public class TaskPublish extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "task_no", nullable = false, unique = true, length = 32)
  private String taskNo;

  @ManyToOne
  @JoinColumn(name = "publisher_id", nullable = false)
  private UserAccount publisher;

  @Column(name = "title", nullable = false, length = 128)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "category", length = 32)
  private String category;

  @Column(name = "location_text", length = 256)
  private String locationText;

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "total_slots", nullable = false)
  private int totalSlots;

  @Column(name = "accepted_slots", nullable = false)
  private int acceptedSlots;

  @Column(name = "deadline_at", nullable = false)
  private Instant deadlineAt;

  @Column(name = "proof_requirements", length = 256)
  private String proofRequirements;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private TaskStatus status;

  @Column(name = "reject_reason", length = 256)
  private String rejectReason;

  @Column(name = "frozen_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal frozenAmount = BigDecimal.ZERO;

  @Column(name = "publish_fee_settled", nullable = false)
  private boolean publishFeeSettled;
}
