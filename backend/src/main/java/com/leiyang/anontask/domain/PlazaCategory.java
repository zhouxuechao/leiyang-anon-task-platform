package com.leiyang.anontask.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plaza_category")
@Getter
@Setter
public class PlazaCategory extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 64)
  private String name;

  @Column(name = "keywords", length = 512)
  private String keywords;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @Column(name = "sort_no", nullable = false)
  private int sortNo;
}
