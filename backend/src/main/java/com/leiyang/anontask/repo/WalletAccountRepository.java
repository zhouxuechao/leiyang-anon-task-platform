package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.WalletAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {
  Optional<WalletAccount> findByUser(UserAccount user);
}

