package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.TaskOrder;
import com.leiyang.anontask.domain.TaskSubmissionLike;
import com.leiyang.anontask.domain.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskSubmissionLikeRepository extends JpaRepository<TaskSubmissionLike, Long> {
  Optional<TaskSubmissionLike> findByOrderAndUser(TaskOrder order, UserAccount user);
  long countByOrder(TaskOrder order);
  long countByOrderId(long orderId);

  @Query("""
      select l.order.id, count(l.id)
      from TaskSubmissionLike l
      where l.order in :orders
      group by l.order.id
      """)
  List<Object[]> countGroupByOrder(@Param("orders") Collection<TaskOrder> orders);

  @Query("""
      select l.order.id
      from TaskSubmissionLike l
      where l.order in :orders and l.user = :user
      """)
  List<Long> findLikedOrderIds(@Param("orders") Collection<TaskOrder> orders, @Param("user") UserAccount user);
}
