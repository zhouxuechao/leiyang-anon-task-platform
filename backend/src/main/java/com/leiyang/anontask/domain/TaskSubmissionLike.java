package com.leiyang.anontask.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "task_submission_like",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_like_order_user", columnNames = {"order_id", "user_id"})
    }
)
@Getter
@Setter
public class TaskSubmissionLike extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "order_id", nullable = false)
  private TaskOrder order;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;
}
