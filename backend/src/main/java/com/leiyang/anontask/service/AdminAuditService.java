package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.config.AsyncMessagingConfig;
import com.leiyang.anontask.domain.TaskOrder;
import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.enums.OrderStatus;
import com.leiyang.anontask.domain.enums.TaskStatus;
import com.leiyang.anontask.repo.TaskOrderRepository;
import com.leiyang.anontask.repo.TaskPublishRepository;
import com.leiyang.anontask.repo.TaskSubmissionLikeRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {
  private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

  private final TaskPublishRepository taskRepo;
  private final TaskOrderRepository orderRepo;
  private final TaskSubmissionLikeRepository submissionLikeRepo;
  private final WalletService walletService;
  private final MessageService messageService;
  private final MessageQueueService queueService;
  private final RedisSupportService redisSupport;

  public AdminAuditService(
      TaskPublishRepository taskRepo,
      TaskOrderRepository orderRepo,
      TaskSubmissionLikeRepository submissionLikeRepo,
      WalletService walletService,
      MessageService messageService,
      MessageQueueService queueService,
      RedisSupportService redisSupport
  ) {
    this.taskRepo = taskRepo;
    this.orderRepo = orderRepo;
    this.submissionLikeRepo = submissionLikeRepo;
    this.walletService = walletService;
    this.messageService = messageService;
    this.queueService = queueService;
    this.redisSupport = redisSupport;
  }

  public record TaskSettleEvent(String taskNo, Instant createdAt) {}

  @Transactional
  public void auditTask(String taskNo, String result, String reason) {
    TaskPublish t = taskRepo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    if (t.getStatus() != TaskStatus.PENDING_AUDIT) {
      throw new BizException("Task is not pending audit");
    }
    if ("APPROVE".equalsIgnoreCase(result)) {
      t.setStatus(TaskStatus.PUBLISHED);
      t.setRejectReason(null);
      if (!t.isPublishFeeSettled()) {
        walletService.settleTaskPublishFreeze(t.getPublisher(), t.getFrozenAmount(), t.getTaskNo());
        t.setPublishFeeSettled(true);
      }
      messageService.send(t.getPublisher(), "AUDIT", "任务发布审核通过", "任务 " + t.getTaskNo() + " 已审核通过并发布。");
    } else if ("REJECT".equalsIgnoreCase(result)) {
      t.setStatus(TaskStatus.REJECTED);
      t.setRejectReason(reason);
      if (!t.isPublishFeeSettled()) {
        walletService.unfreezeTaskPublish(t.getPublisher(), t.getFrozenAmount(), t.getTaskNo());
        t.setPublishFeeSettled(true);
      }
      messageService.send(t.getPublisher(), "AUDIT", "任务发布审核驳回", "任务 " + t.getTaskNo() + " 未通过审核。原因：" + safe(reason));
    } else {
      throw new BizException("Invalid audit result");
    }
    taskRepo.save(t);
    log.info("task_audited taskNo={} result={} status={} publisherId={}", taskNo, result, t.getStatus(), t.getPublisher().getId());
  }

  @Transactional
  public void auditOrder(String orderNo, String result, String reason) {
    TaskOrder o = orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new BizException("Order not found"));
    if (o.getOrderStatus() != OrderStatus.SUBMITTED) {
      throw new BizException("Order is not submitted");
    }
    if ("APPROVE".equalsIgnoreCase(result)) {
      o.setOrderStatus(OrderStatus.APPROVED);
      o.setAuditReason(null);
      orderRepo.save(o);
      messageService.send(
          o.getAcceptUser(),
          "AUDIT",
          "任务审核通过",
          "订单 " + o.getOrderNo() + " 已审核通过，任务截止后将按点赞权重自动结算奖励。"
      );
      log.info("task_order_audited orderNo={} result={} status={} userId={}", orderNo, result, o.getOrderStatus(), o.getAcceptUser().getId());
      return;
    }
    if ("REJECT_RESUBMIT".equalsIgnoreCase(result)) {
      o.setOrderStatus(OrderStatus.REJECTED_RESUBMIT);
      o.setAuditReason(reason);
      orderRepo.save(o);
      messageService.send(o.getAcceptUser(), "AUDIT", "任务凭证驳回", "订单 " + o.getOrderNo() + " 驳回，可重新提交。原因：" + safe(reason));
      log.info("task_order_audited orderNo={} result={} status={} userId={}", orderNo, result, o.getOrderStatus(), o.getAcceptUser().getId());
      return;
    }
    if ("REJECT_CLOSE".equalsIgnoreCase(result)) {
      o.setOrderStatus(OrderStatus.REJECTED_CLOSE);
      o.setAuditReason(reason);
      orderRepo.save(o);
      messageService.send(o.getAcceptUser(), "AUDIT", "任务凭证驳回并关闭", "订单 " + o.getOrderNo() + " 驳回并关闭。原因：" + safe(reason));
      log.info("task_order_audited orderNo={} result={} status={} userId={}", orderNo, result, o.getOrderStatus(), o.getAcceptUser().getId());
      return;
    }
    throw new BizException("Invalid audit result");
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  private BigDecimal calculateWeightedReward(TaskOrder current) {
    TaskPublish task = current.getTask();
    List<TaskOrder> settled = orderRepo.findByTaskAndOrderStatusIn(task, List.of(OrderStatus.SETTLED));
    java.util.LinkedHashSet<TaskOrder> completed = new java.util.LinkedHashSet<>(settled);
    completed.add(current);

    if (completed.isEmpty()) {
      return task.getAmount();
    }

    long totalWeight = 0L;
    long currentWeight = 1L;
    for (TaskOrder o : completed) {
      long likes = submissionLikeRepo.countByOrderId(o.getId());
      long weight = 1L + likes;
      totalWeight += weight;
      if (o.getId().equals(current.getId())) {
        currentWeight = weight;
      }
    }
    if (totalWeight <= 0) {
      return task.getAmount();
    }

    BigDecimal totalBudget = task.getAmount();
    return totalBudget
        .multiply(BigDecimal.valueOf(currentWeight))
        .divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
  }

  @Transactional
  public int autoSettleExpiredTask(TaskPublish task) {
    if (task == null || task.getTaskNo() == null) return 0;
    if (!redisSupport.setIfAbsent("lock:task:settle:" + task.getTaskNo(), String.valueOf(System.currentTimeMillis()), Duration.ofMinutes(10))) {
      log.info("task_auto_settle_skipped_duplicate taskNo={}", task.getTaskNo());
      return 0;
    }
    List<TaskOrder> toSettle = orderRepo.findByTaskAndOrderStatusIn(task, List.of(OrderStatus.SUBMITTED, OrderStatus.APPROVED));
    if (toSettle.isEmpty()) return 0;

    List<TaskOrder> settled = orderRepo.findByTaskAndOrderStatusIn(task, List.of(OrderStatus.SETTLED));
    java.util.LinkedHashSet<TaskOrder> completedSet = new java.util.LinkedHashSet<>(settled);
    completedSet.addAll(toSettle);
    List<TaskOrder> completed = new java.util.ArrayList<>(completedSet);

    Map<Long, Long> likeMap = new HashMap<>();
    long totalWeight = 0L;
    for (TaskOrder o : completed) {
      long likes = submissionLikeRepo.countByOrderId(o.getId());
      long weight = 1L + likes;
      likeMap.put(o.getId(), weight);
      totalWeight += weight;
    }
    if (totalWeight <= 0) totalWeight = completed.size();
    BigDecimal totalBudget = task.getAmount();

    Instant now = Instant.now();
    for (TaskOrder o : toSettle) {
      long weight = likeMap.getOrDefault(o.getId(), 1L);
      BigDecimal reward = totalBudget
          .multiply(BigDecimal.valueOf(weight))
          .divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
      o.setOrderStatus(OrderStatus.SETTLED);
      o.setSettledTime(now);
      o.setSettledAmount(reward);
      o.setAuditReason(null);
      orderRepo.save(o);
      walletService.creditTaskReward(o.getAcceptUser(), reward, o.getOrderNo());
      messageService.send(
          o.getAcceptUser(),
          "AUDIT",
          "任务自动结算",
          "订单 " + o.getOrderNo() + " 已在任务截止后自动结算，发放奖励 ¥" + reward + "（按点赞权重）。"
      );
    }
    log.info("task_auto_settled taskNo={} orderCount={}", task.getTaskNo(), toSettle.size());
    return toSettle.size();
  }

  @Transactional
  public int autoSettleExpiredTask(String taskNo) {
    TaskPublish task = taskRepo.findByTaskNo(taskNo).orElse(null);
    if (task == null) return 0;
    return autoSettleExpiredTask(task);
  }

  public boolean enqueueAutoSettle(String taskNo) {
    return queueService.publish(AsyncMessagingConfig.TASK_SETTLE_ROUTING_KEY, new TaskSettleEvent(taskNo, Instant.now()));
  }
}
