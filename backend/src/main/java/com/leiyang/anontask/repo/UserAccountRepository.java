package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByOpenId(String openId);
}

