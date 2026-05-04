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
@Table(name = "user_message")
@Getter
@Setter
public class UserMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "msg_type", nullable = false, length = 32)
  private String msgType;

  @Column(name = "title", nullable = false, length = 128)
  private String title;

  @Column(name = "content", nullable = false, length = 512)
  private String content;

  @Column(name = "read_flag", nullable = false)
  private boolean readFlag;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}

