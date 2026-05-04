package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.UserMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {
  List<UserMessage> findTop50ByUserOrderByCreatedAtDesc(UserAccount user);
  long countByUserAndReadFlagFalse(UserAccount user);
  List<UserMessage> findByUserAndReadFlagFalse(UserAccount user);
}
