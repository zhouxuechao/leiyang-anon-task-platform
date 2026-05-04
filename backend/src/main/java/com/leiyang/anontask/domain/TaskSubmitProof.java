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
@Table(name = "task_submit_proof")
@Getter
@Setter
public class TaskSubmitProof {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "order_id", nullable = false)
  private TaskOrder order;

  @Column(name = "proof_type", nullable = false, length = 32)
  private String proofType;

  @Column(name = "proof_url", nullable = false, length = 1024)
  private String proofUrl;

  @Column(name = "remark", length = 256)
  private String remark;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

