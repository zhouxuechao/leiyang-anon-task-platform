package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.UserAuth;
import com.leiyang.anontask.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
  Optional<UserAuth> findByUser(UserAccount user);
  Optional<UserAuth> findByMobile(String mobile);
  Optional<UserAuth> findByEmailIgnoreCase(String email);
}
