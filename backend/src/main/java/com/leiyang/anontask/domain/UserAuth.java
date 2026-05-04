package com.leiyang.anontask.domain;

import com.leiyang.anontask.domain.enums.AuthStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_auth")
@Getter
@Setter
public class UserAuth extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserAccount user;

  @Column(name = "real_name", length = 64)
  private String realName;

  @Column(name = "id_no_enc", length = 256)
  private String idNoEnc;

  @Column(name = "mobile", length = 32)
  private String mobile;

  @Column(name = "email", length = 128)
  private String email;

  @Column(name = "password_hash", length = 128)
  private String passwordHash;

  @Column(name = "pay_account", length = 128)
  private String payAccount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private AuthStatus status;
}
