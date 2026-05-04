package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.config.AppProperties;
import com.leiyang.anontask.domain.TaskOrder;
import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.TaskSubmitProof;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.UserAuth;
import com.leiyang.anontask.domain.enums.AuthStatus;
import com.leiyang.anontask.domain.enums.OrderStatus;
import com.leiyang.anontask.domain.enums.SensitiveWordAction;
import com.leiyang.anontask.domain.enums.TaskStatus;
import com.leiyang.anontask.dto.mp.AcceptTaskResponse;
import com.leiyang.anontask.dto.mp.OrderListItem;
import com.leiyang.anontask.dto.mp.SubmitOrderRequest;
import com.leiyang.anontask.dto.mp.TaskCreateRequest;
import com.leiyang.anontask.dto.mp.TaskCommentItem;
import com.leiyang.anontask.dto.mp.TaskDetailResponse;
import com.leiyang.anontask.dto.mp.TaskListItem;
import com.leiyang.anontask.dto.mp.TaskSubmissionItem;
import com.leiyang.anontask.dto.mp.TaskSubmissionItem.ProofItem;
import com.leiyang.anontask.dto.mp.SliceResponse;
import com.leiyang.anontask.domain.TaskComment;
import com.leiyang.anontask.repo.TaskOrderRepository;
import com.leiyang.anontask.repo.TaskCommentRepository;
import com.leiyang.anontask.repo.TaskPublishRepository;
import com.leiyang.anontask.repo.TaskSubmissionLikeRepository;
import com.leiyang.anontask.repo.TaskSubmitProofRepository;
import com.leiyang.anontask.repo.UserAuthRepository;
import com.leiyang.anontask.util.NoGenerator;
import com.leiyang.anontask.util.Slices;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MpTaskService {
  private static final Logger log = LoggerFactory.getLogger(MpTaskService.class);

  private final AppProperties props;
  private final TaskPublishRepository taskRepo;
  private final TaskOrderRepository orderRepo;
  private final TaskSubmitProofRepository proofRepo;
  private final TaskCommentRepository commentRepo;
  private final TaskSubmissionLikeRepository submissionLikeRepo;
  private final UserAuthRepository authRepo;
  private final SensitiveWordService sensitiveWordService;
  private final ConfigService configService;
  private final WalletService walletService;
  private final RedisSupportService redisSupport;

  public MpTaskService(
      AppProperties props,
      TaskPublishRepository taskRepo,
      TaskOrderRepository orderRepo,
      TaskSubmitProofRepository proofRepo,
      TaskCommentRepository commentRepo,
      TaskSubmissionLikeRepository submissionLikeRepo,
      UserAuthRepository authRepo,
      SensitiveWordService sensitiveWordService,
      ConfigService configService,
      WalletService walletService,
      RedisSupportService redisSupport
  ) {
    this.props = props;
    this.taskRepo = taskRepo;
    this.orderRepo = orderRepo;
    this.proofRepo = proofRepo;
    this.commentRepo = commentRepo;
    this.submissionLikeRepo = submissionLikeRepo;
    this.authRepo = authRepo;
    this.sensitiveWordService = sensitiveWordService;
    this.configService = configService;
    this.walletService = walletService;
    this.redisSupport = redisSupport;
  }

  public List<TaskListItem> listPublished(UserAccount viewer, int page, int size, String q, String sort) {
    Instant now = Instant.now();
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 50);
    String query = q == null || q.isBlank() ? null : q.trim();
    String sortKey = sort == null ? "LATEST" : sort.trim().toUpperCase();
    Sort order = buildTaskSort(sortKey);
    var tasks = "ENDED".equals(sortKey)
        ? taskRepo.findEndedItems(now, query, PageRequest.of(safePage - 1, safeSize, order))
        : taskRepo.findLatestPublishedItems(TaskStatus.PUBLISHED, now, query, PageRequest.of(safePage - 1, safeSize, order));

    final java.util.Map<Long, TaskOrder> myOrderMap = (viewer != null && !tasks.isEmpty())
        ? orderRepo.findByAcceptUserAndTaskIn(viewer, tasks).stream().collect(
            java.util.stream.Collectors.toMap(
                o -> o.getTask().getId(),
                o -> o,
                (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
            )
        )
        : java.util.Collections.emptyMap();

    return tasks.stream()
        .map(t -> toTaskListItem(t, myOrderMap.get(t.getId())))
        .toList();
  }

  public SliceResponse<TaskListItem> listPublishedSlice(UserAccount viewer, int page, int size, String q, String sort) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 50);
    List<TaskListItem> rows = listPublished(viewer, safePage, safeSize + 1, q, sort);
    return Slices.of(rows, safePage, safeSize);
  }

  public List<TaskListItem> listUserPublishedTasks(long userId, int page, int size) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 50);
    return taskRepo.findPublisherItems(
            userId,
            TaskStatus.PUBLISHED,
            PageRequest.of(safePage - 1, safeSize)
        )
        .stream()
        .map(t -> toTaskListItem(t, null))
        .toList();
  }

  public SliceResponse<TaskListItem> listUserPublishedTasksSlice(long userId, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 50);
    List<TaskListItem> rows = listUserPublishedTasks(userId, safePage, safeSize + 1);
    return Slices.of(rows, safePage, safeSize);
  }

  public record MyOrderState(
      String taskNo,
      String orderNo,
      String orderStatus,
      Instant acceptTime,
      Instant submitTime
  ) {}

  public MyOrderState getMyOrderState(UserAccount user, String taskNo) {
    TaskPublish t = taskRepo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    var orders = orderRepo.findByTaskAndAcceptUserOrderByCreatedAtDesc(t, user);
    var order = orders.isEmpty() ? null : orders.get(0);
    if (order == null) return null;
    return new MyOrderState(
        t.getTaskNo(),
        order.getOrderNo(),
        order.getOrderStatus().name(),
        order.getAcceptTime(),
        order.getSubmitTime()
    );
  }

  private Sort buildTaskSort(String sort) {
    String key = sort == null ? "LATEST" : sort.trim().toUpperCase();
    return switch (key) {
      case "AMOUNT" -> Sort.by(
          Sort.Order.desc("amount"),
          Sort.Order.desc("createdAt"),
          Sort.Order.desc("totalSlots")
      );
      case "PEOPLE" -> Sort.by(
          Sort.Order.desc("totalSlots"),
          Sort.Order.desc("amount"),
          Sort.Order.desc("createdAt")
      );
      case "ENDED" -> Sort.by(
          Sort.Order.desc("deadlineAt"),
          Sort.Order.desc("createdAt")
      );
      default -> Sort.by(
          Sort.Order.desc("createdAt"),
          Sort.Order.desc("amount"),
          Sort.Order.desc("totalSlots")
      );
    };
  }

  private TaskListItem toTaskListItem(TaskPublish t, TaskOrder myOrder) {
    return new TaskListItem(
        t.getTaskNo(),
        t.getTitle(),
        t.getCategory(),
        t.getAmount(),
        t.getLocationText(),
        t.getTotalSlots(),
        t.getAcceptedSlots(),
        t.getCreatedAt(),
        t.getDeadlineAt(),
        t.getStatus().name(),
        myOrder == null ? null : myOrder.getOrderNo(),
        myOrder == null ? null : myOrder.getOrderStatus().name()
    );
  }

  public TaskDetailResponse getDetail(String taskNo) {
    TaskPublish t = taskRepo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    return new TaskDetailResponse(
        t.getTaskNo(),
        t.getTitle(),
        t.getContent(),
        t.getCategory(),
        t.getLocationText(),
        t.getAmount(),
        t.getTotalSlots(),
        t.getAcceptedSlots(),
        t.getCreatedAt(),
        t.getDeadlineAt(),
        t.getProofRequirements(),
        t.getStatus().name()
    );
  }

  public List<TaskSubmissionItem> listSubmissions(String taskNo, UserAccount viewer) {
    TaskPublish t = taskRepo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    var rawOrders = orderRepo.findTop20ByTaskAndSubmitTimeIsNotNullOrderBySubmitTimeDesc(t);
    List<TaskOrder> orders = new ArrayList<>();
    for (var o : rawOrders) {
      var st = o.getOrderStatus();
      boolean ownerVisible = viewer != null && o.getAcceptUser().getId().equals(viewer.getId());
      boolean publicVisible = st == OrderStatus.APPROVED || st == OrderStatus.SETTLED;
      if (ownerVisible || publicVisible) {
        orders.add(o);
      }
    }
    var likeStats = submissionLikeRepo.countGroupByOrder(orders).stream().collect(
        java.util.stream.Collectors.toMap(
            it -> ((Number) it[0]).longValue(),
            it -> ((Number) it[1]).longValue()
        )
    );
    java.util.Set<Long> likedIds = java.util.Collections.emptySet();
    if (viewer != null && !orders.isEmpty()) {
      likedIds = new java.util.HashSet<>(submissionLikeRepo.findLikedOrderIds(orders, viewer));
    }
    List<TaskSubmissionItem> out = new ArrayList<>();
    for (var o : orders) {
      var proofs = proofRepo.findByOrderOrderByIdAsc(o).stream()
          .map(p -> new ProofItem(p.getProofType(), p.getProofUrl(), p.getRemark()))
          .toList();
      out.add(new TaskSubmissionItem(
          o.getOrderNo(),
          toDisplayName(o.getAcceptUser()),
          safeAvatar(o.getAcceptUser().getAvatar()),
          o.getOrderStatus().name(),
          o.getSubmitTime(),
          o.getSettledAmount() == null ? "" : o.getSettledAmount().toPlainString(),
          likeStats.getOrDefault(o.getId(), 0L),
          likedIds.contains(o.getId()),
          proofs
      ));
    }
    return out;
  }

  @Transactional
  public LikeResult setSubmissionLike(UserAccount user, String taskNo, String orderNo, boolean wantLike) {
    TaskPublish t = taskRepo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    TaskOrder o = orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new BizException("Order not found"));
    if (!o.getTask().getId().equals(t.getId())) {
      throw new BizException("Order not in task");
    }
    if (o.getSubmitTime() == null) {
      throw new BizException("Order has no submission");
    }
    if (o.getAcceptUser().getId().equals(user.getId())) {
      throw new BizException("Cannot like your own submission");
    }
    var existed = submissionLikeRepo.findByOrderAndUser(o, user);
    if (wantLike) {
      if (existed.isEmpty()) {
        var like = new com.leiyang.anontask.domain.TaskSubmissionLike();
        like.setOrder(o);
        like.setUser(user);
        submissionLikeRepo.save(like);
      }
    } else {
      existed.ifPresent(submissionLikeRepo::delete);
    }
    long likeCount = submissionLikeRepo.countByOrder(o);
    return new LikeResult(wantLike, likeCount);
  }

  public List<TaskCommentItem> listComments(String taskNo) {
    TaskPublish t = taskRepo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    return commentRepo.findTop50ByTaskOrderByCreatedAtDesc(t).stream()
        .map(c -> new TaskCommentItem(
            c.getId(),
            toDisplayName(c.getUser()),
            c.getContent(),
            c.getCreatedAt()
        ))
        .toList();
  }

  @Transactional
  public void addComment(UserAccount user, String taskNo, String content) {
    TaskPublish t = taskRepo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    String v = String.valueOf(content == null ? "" : content).trim();
    if (v.isEmpty()) throw new BizException("content is required");
    if (v.length() > 512) throw new BizException("content too long");
    SensitiveWordAction a = sensitiveWordService.evaluate(v);
    if (a == SensitiveWordAction.REJECT) throw new BizException("Comment contains sensitive content");
    TaskComment c = new TaskComment();
    c.setTask(t);
    c.setUser(user);
    c.setContent(v);
    commentRepo.save(c);
  }

  @Transactional
  public String createTask(UserAccount publisher, TaskCreateRequest req) {
    if (!redisSupport.setIfAbsent("lock:task:create:user:" + publisher.getId(), String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(30))) {
      throw new BizException("任务正在提交，请勿重复点击");
    }
    SensitiveWordAction a1 = sensitiveWordService.evaluate(req.title());
    SensitiveWordAction a2 = sensitiveWordService.evaluate(req.content());
    if (a1 == SensitiveWordAction.REJECT || a2 == SensitiveWordAction.REJECT) {
      throw new BizException("Task contains sensitive content");
    }
    TaskPublish t = new TaskPublish();
    t.setTaskNo(NoGenerator.gen("T"));
    walletService.freezeForTaskPublish(publisher, req.amount(), t.getTaskNo());
    t.setPublisher(publisher);
    t.setTitle(req.title());
    t.setContent(req.content());
    t.setCategory(req.category());
    t.setLocationText(req.locationText());
    t.setAmount(req.amount());
    t.setTotalSlots(req.totalSlots());
    t.setAcceptedSlots(0);
    t.setDeadlineAt(req.deadlineAt());
    t.setProofRequirements(req.proofRequirements());
    t.setStatus(TaskStatus.PENDING_AUDIT);
    t.setFrozenAmount(req.amount());
    t.setPublishFeeSettled(false);
    taskRepo.save(t);
    log.info("task_created taskNo={} publisherId={} amount={} slots={} deadline={}", t.getTaskNo(), publisher.getId(), t.getAmount(), t.getTotalSlots(), t.getDeadlineAt());
    return t.getTaskNo();
  }

  @Transactional
  public AcceptTaskResponse acceptTask(UserAccount user, String taskNo) {
    if (!redisSupport.setIfAbsent("lock:task:accept:" + taskNo + ":user:" + user.getId(), String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(10))) {
      throw new BizException("正在接单，请勿重复点击");
    }
    long ongoing = orderRepo.countByAcceptUserAndOrderStatusIn(
        user,
        List.of(OrderStatus.ACCEPTED, OrderStatus.SUBMITTED, OrderStatus.APPROVED)
    );
    int maxOngoing = configService.getInt("limits.maxOngoingOrders", props.limits().maxOngoingOrders());
    if (ongoing >= maxOngoing) {
      throw new BizException("Too many ongoing orders");
    }

    TaskPublish t = taskRepo.findByTaskNoForUpdate(taskNo).orElseThrow(() -> new BizException("Task not found"));
    if (t.getStatus() != TaskStatus.PUBLISHED) {
      throw new BizException("Task is not published");
    }
    if (t.getDeadlineAt().isBefore(Instant.now())) {
      throw new BizException("Task expired");
    }
    if (t.getAcceptedSlots() >= t.getTotalSlots()) {
      throw new BizException("No slots left");
    }
    if (orderRepo.findByTaskAndAcceptUser(t, user).isPresent()) {
      throw new BizException("Already accepted");
    }

    t.setAcceptedSlots(t.getAcceptedSlots() + 1);
    taskRepo.save(t);

    TaskOrder o = new TaskOrder();
    o.setOrderNo(NoGenerator.gen("O"));
    o.setTask(t);
    o.setAcceptUser(user);
    o.setOrderStatus(OrderStatus.ACCEPTED);
    o.setAcceptTime(Instant.now());
    orderRepo.save(o);
    log.info("task_accepted taskNo={} orderNo={} userId={} acceptedSlots={}/{}", t.getTaskNo(), o.getOrderNo(), user.getId(), t.getAcceptedSlots(), t.getTotalSlots());

    return new AcceptTaskResponse(o.getOrderNo(), o.getOrderStatus().name());
  }

  @Transactional
  public void submitOrder(UserAccount user, String orderNo, SubmitOrderRequest req) {
    if (!redisSupport.setIfAbsent("lock:task:submit:" + orderNo + ":user:" + user.getId(), String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(30))) {
      throw new BizException("凭证正在提交，请勿重复点击");
    }
    TaskOrder o = orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new BizException("Order not found"));
    if (!o.getAcceptUser().getId().equals(user.getId())) {
      throw new BizException("Forbidden");
    }
    if (o.getOrderStatus() != OrderStatus.ACCEPTED && o.getOrderStatus() != OrderStatus.REJECTED_RESUBMIT) {
      throw new BizException("Order status does not allow submit");
    }
    proofRepo.deleteByOrder(o);
    var proofs = req.proofs() == null ? List.<com.leiyang.anontask.dto.mp.SubmitProofItem>of() : req.proofs();
    for (var p : proofs) {
      TaskSubmitProof proof = new TaskSubmitProof();
      proof.setOrder(o);
      proof.setProofType(p.type());
      proof.setProofUrl(p.url());
      proof.setRemark(p.remark());
      proof.setCreatedAt(Instant.now());
      proofRepo.save(proof);
    }
    o.setOrderStatus(OrderStatus.SUBMITTED);
    o.setSubmitTime(Instant.now());
    orderRepo.save(o);
    log.info("task_order_submitted orderNo={} taskNo={} userId={} proofCount={}", o.getOrderNo(), o.getTask().getTaskNo(), user.getId(), proofs.size());
  }

  public List<OrderListItem> myOrders(UserAccount user, int page, int size) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 50);
    return orderRepo.findAcceptUserItems(
            user,
            PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createdAt")))
        )
        .stream()
        .map(o -> new OrderListItem(
            o.getOrderNo(),
            o.getTask().getTaskNo(),
            o.getTask().getTitle(),
            o.getTask().getAmount(),
            o.getOrderStatus().name(),
            o.getAcceptTime(),
            o.getSubmitTime(),
            o.getAuditReason()
        ))
        .toList();
  }

  public SliceResponse<OrderListItem> myOrdersSlice(UserAccount user, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 50);
    List<OrderListItem> rows = myOrders(user, safePage, safeSize + 1);
    return Slices.of(rows, safePage, safeSize);
  }

  private void requireVerified(UserAccount user) {
    UserAuth auth = authRepo.findByUser(user).orElse(null);
    if (auth == null || auth.getStatus() != AuthStatus.VERIFIED) {
      throw new BizException("Real-name verification required");
    }
  }

  private String toDisplayName(UserAccount u) {
    String nick = u.getNickname();
    if (nick != null && !nick.isBlank()) return nick;
    String openId = u.getOpenId();
    if (openId == null || openId.isBlank()) return "用户";
    String tail = openId.length() <= 4 ? openId : openId.substring(openId.length() - 4);
    return "用户" + tail;
  }

  private static String safeAvatar(String avatar) {
    if (avatar == null) return "";
    String v = avatar.trim();
    if (v.isEmpty()) return "";
    return v;
  }

  public record LikeResult(boolean liked, long likeCount) {}
}
