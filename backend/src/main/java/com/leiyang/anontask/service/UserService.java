package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.repo.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserAccountRepository repo;

  public UserService(UserAccountRepository repo) {
    this.repo = repo;
  }

  public UserAccount requireUser(long id) {
    return repo.findById(id).orElseThrow(() -> new BizException("User not found"));
  }
}

