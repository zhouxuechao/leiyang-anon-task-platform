package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.MusicCreditLedger;
import com.leiyang.anontask.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MusicCreditLedgerRepository extends JpaRepository<MusicCreditLedger, Long> {
  @Query("select coalesce(sum(l.changeAmount), 0) from MusicCreditLedger l where l.user = :user")
  long sumByUser(@Param("user") UserAccount user);
}
