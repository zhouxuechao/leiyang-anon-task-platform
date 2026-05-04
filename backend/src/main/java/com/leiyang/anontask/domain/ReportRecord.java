package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.ReportStatus;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_record")
@Getter
@Setter
public class ReportRecord extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "report_no", nullable = false, unique = true, length = 32)
  private String reportNo;

  @ManyToOne
  @JoinColumn(name = "reporter_id", nullable = false)
  private UserAccount reporter;

  @Column(name = "target_type", nullable = false, length = 32)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @Column(name = "reason", nullable = false, length = 256)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ReportStatus status;
}

