package com.leiyang.anontask.job;

import com.leiyang.anontask.domain.TaskOrder;
import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.enums.OrderStatus;
import com.leiyang.anontask.domain.enums.TaskStatus;
import com.leiyang.anontask.repo.TaskOrderRepository;
import com.leiyang.anontask.repo.TaskPublishRepository;
import com.leiyang.anontask.service.AdminAuditService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LifecycleJob {
  private final TaskPublishRepository taskRepo;
  private final TaskOrderRepository orderRepo;
  private final AdminAuditService auditService;

  public LifecycleJob(TaskPublishRepository taskRepo, TaskOrderRepository orderRepo, AdminAuditService auditService) {
    this.taskRepo = taskRepo;
    this.orderRepo = orderRepo;
    this.auditService = auditService;
  }

  @Scheduled(fixedDelayString = "PT60S")
  @Transactional
  public void expireTasksAndOrders() {
    Instant now = Instant.now();

    List<TaskPublish> expiredTasks = taskRepo.findPublishedExpired(now);
    for (TaskPublish t : expiredTasks) {
      t.setStatus(TaskStatus.EXPIRED);
      taskRepo.save(t);
      if (!auditService.enqueueAutoSettle(t.getTaskNo())) {
        auditService.autoSettleExpiredTask(t);
      }
    }

    List<TaskOrder> timeoutOrders = orderRepo.findAcceptedTimeout(now);
    for (TaskOrder o : timeoutOrders) {
      o.setOrderStatus(OrderStatus.TIMEOUT);
      orderRepo.save(o);
    }
  }
}
