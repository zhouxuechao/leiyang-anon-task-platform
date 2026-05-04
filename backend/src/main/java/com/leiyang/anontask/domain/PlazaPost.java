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
@Table(name = "plaza_post")
@Getter
@Setter
public class PlazaPost extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "author_id", nullable = false)
  private UserAccount author;

  @Column(name = "content", nullable = false, length = 500)
  private String content;

  @Column(name = "gender", nullable = false, length = 16)
  private String gender;

  @Column(name = "category", nullable = false, length = 32)
  private String category;

  @Column(name = "like_count", nullable = false)
  private int likeCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  @Column(name = "hot_flag", nullable = false)
  private boolean hotFlag;
}
