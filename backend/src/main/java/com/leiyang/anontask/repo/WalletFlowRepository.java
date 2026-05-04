package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.WalletFlow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletFlowRepository extends JpaRepository<WalletFlow, Long> {
  List<WalletFlow> findTop50ByUserOrderByCreatedAtDesc(UserAccount user);
}

