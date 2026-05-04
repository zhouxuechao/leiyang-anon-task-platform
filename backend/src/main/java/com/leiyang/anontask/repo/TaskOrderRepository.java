package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.TaskOrder;
import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.enums.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskOrderRepository extends JpaRepository<TaskOrder, Long> {
  Optional<TaskOrder> findByOrderNo(String orderNo);

  Optional<TaskOrder> findByTaskAndAcceptUser(TaskPublish task, UserAccount acceptUser);
  List<TaskOrder> findByTaskAndAcceptUserOrderByCreatedAtDesc(TaskPublish task, UserAccount acceptUser);

  List<TaskOrder> findTop50ByAcceptUserOrderByCreatedAtDesc(UserAccount acceptUser);

  Page<TaskOrder> findByAcceptUserOrderByCreatedAtDesc(UserAccount acceptUser, Pageable pageable);

  @Query("select o from TaskOrder o where o.acceptUser = :acceptUser")
  List<TaskOrder> findAcceptUserItems(@Param("acceptUser") UserAccount acceptUser, Pageable pageable);

  long countByAcceptUserAndOrderStatusIn(UserAccount acceptUser, List<OrderStatus> statuses);

  List<TaskOrder> findTop50ByOrderStatusOrderByCreatedAtDesc(OrderStatus status);

  Page<TaskOrder> findByOrderStatus(OrderStatus status, Pageable pageable);

  List<TaskOrder> findTop20ByTaskAndSubmitTimeIsNotNullOrderBySubmitTimeDesc(TaskPublish task);
  List<TaskOrder> findByTaskAndSubmitTimeIsNotNullOrderBySubmitTimeDesc(TaskPublish task);

  List<TaskOrder> findByTaskAndOrderStatusIn(TaskPublish task, List<OrderStatus> statuses);

  List<TaskOrder> findByAcceptUserAndTaskIn(UserAccount acceptUser, List<TaskPublish> tasks);

  @Query("""
      select o from TaskOrder o
      join o.task t
      where o.orderStatus = com.leiyang.anontask.domain.enums.OrderStatus.ACCEPTED
        and t.deadlineAt <= :now
      """)
  List<TaskOrder> findAcceptedTimeout(@Param("now") java.time.Instant now);
}
