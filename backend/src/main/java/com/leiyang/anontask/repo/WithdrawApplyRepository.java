package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.WithdrawApply;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.enums.WithdrawStatus;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawApplyRepository extends JpaRepository<WithdrawApply, Long> {
  Optional<WithdrawApply> findByApplyNo(String applyNo);
  List<WithdrawApply> findTop50ByAuditStatusOrderByCreatedAtDesc(WithdrawStatus status);
  List<WithdrawApply> findTop50ByUserOrderByCreatedAtDesc(UserAccount user);
  Page<WithdrawApply> findByAuditStatusOrderByCreatedAtDesc(WithdrawStatus status, Pageable pageable);

  @Query("""
      select w from WithdrawApply w
      where (:status is null or w.auditStatus = :status)
        and (
          :q = '' or lower(w.applyNo) like lower(concat('%', :q, '%'))
          or lower(coalesce(w.user.openId, '')) like lower(concat('%', :q, '%'))
        )
      order by w.createdAt desc
      """)
  Page<WithdrawApply> adminSearch(@Param("status") WithdrawStatus status, @Param("q") String q, Pageable pageable);

  long countByAuditStatus(WithdrawStatus status);
  long countByAuditStatusAndCreatedAtGreaterThanEqual(WithdrawStatus status, Instant from);
  long countByAuditStatusAndPaidAtGreaterThanEqual(WithdrawStatus status, Instant from);

  @Query("select coalesce(sum(w.amount), 0) from WithdrawApply w where w.auditStatus = :status")
  BigDecimal sumAmountByAuditStatus(@Param("status") WithdrawStatus status);

  @Query("select coalesce(sum(w.amount), 0) from WithdrawApply w where w.auditStatus = :status and w.createdAt >= :from")
  BigDecimal sumAmountByAuditStatusAndCreatedAtGreaterThanEqual(@Param("status") WithdrawStatus status, @Param("from") Instant from);

  @Query("select coalesce(sum(w.amount), 0) from WithdrawApply w where w.auditStatus = :status and w.paidAt >= :from")
  BigDecimal sumAmountByAuditStatusAndPaidAtGreaterThanEqual(@Param("status") WithdrawStatus status, @Param("from") Instant from);
}
