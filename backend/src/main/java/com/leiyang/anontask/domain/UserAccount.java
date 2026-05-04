package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_account")
@Getter
@Setter
public class UserAccount extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "open_id", nullable = false, unique = true, length = 64)
  private String openId;

  @Column(name = "nickname", length = 64)
  private String nickname;

  @Column(name = "avatar", length = 512)
  private String avatar;

  @Column(name = "gender", length = 16)
  private String gender;

  @Column(name = "signature", length = 255)
  private String signature;

  @Column(name = "withdraw_qr_code_url", length = 4096)
  private String withdrawQrCodeUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private UserStatus status;

  @Column(name = "credit_score", nullable = false)
  private int creditScore;
}
