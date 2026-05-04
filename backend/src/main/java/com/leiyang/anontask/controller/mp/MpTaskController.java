package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.mp.AcceptTaskResponse;
import com.leiyang.anontask.dto.mp.OrderListItem;
import com.leiyang.anontask.dto.mp.SubmitOrderRequest;
import com.leiyang.anontask.dto.mp.SliceResponse;
import com.leiyang.anontask.dto.mp.TaskCreateRequest;
import com.leiyang.anontask.dto.mp.TaskDetailResponse;
import com.leiyang.anontask.dto.mp.TaskListItem;
import com.leiyang.anontask.dto.mp.TaskSubmissionItem;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.MpTaskService;
import com.leiyang.anontask.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mp")
public class MpTaskController {
  private final MpTaskService taskService;
  private final UserService userService;

  public MpTaskController(MpTaskService taskService, UserService userService) {
    this.taskService = taskService;
    this.userService = userService;
  }

  @GetMapping("/tasks")
  public ApiResult<SliceResponse<TaskListItem>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "LATEST") String sort
  ) {
    var viewer = resolveOptionalViewer();
    return ApiResult.ok(taskService.listPublishedSlice(viewer, page, size, q, sort));
  }

  @GetMapping("/tasks/{taskNo}")
  public ApiResult<TaskDetailResponse> detail(@PathVariable String taskNo) {
    return ApiResult.ok(taskService.getDetail(taskNo));
  }

  @GetMapping("/tasks/{taskNo}/my-order")
  public ApiResult<MpTaskService.MyOrderState> myOrderState(@PathVariable String taskNo) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    return ApiResult.ok(taskService.getMyOrderState(user, taskNo));
  }

  @GetMapping("/public/tasks/{taskNo}/submissions")
  public ApiResult<List<TaskSubmissionItem>> submissions(@PathVariable String taskNo) {
    return ApiResult.ok(taskService.listSubmissions(taskNo, null));
  }

  @GetMapping("/tasks/{taskNo}/submissions")
  public ApiResult<List<TaskSubmissionItem>> myViewSubmissions(@PathVariable String taskNo) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    return ApiResult.ok(taskService.listSubmissions(taskNo, user));
  }

  public record SubmissionLikeRequest(boolean like) {}
  public record SubmissionLikeResp(boolean liked, long likeCount) {}

  @PostMapping("/tasks/{taskNo}/submissions/{orderNo}/like")
  public ApiResult<SubmissionLikeResp> likeSubmission(
      @PathVariable String taskNo,
      @PathVariable String orderNo,
      @RequestBody(required = false) SubmissionLikeRequest req
  ) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    boolean wantLike = req == null || req.like();
    var r = taskService.setSubmissionLike(user, taskNo, orderNo, wantLike);
    return ApiResult.ok(new SubmissionLikeResp(r.liked(), r.likeCount()));
  }

  public record CreateResp(String taskNo, String status) {}

  @PostMapping("/tasks")
  public ApiResult<CreateResp> create(@Valid @RequestBody TaskCreateRequest req) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    String taskNo = taskService.createTask(user, req);
    return ApiResult.ok(new CreateResp(taskNo, "PENDING_AUDIT"));
  }

  @PostMapping("/tasks/{taskNo}/accept")
  public ApiResult<AcceptTaskResponse> accept(@PathVariable String taskNo) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    return ApiResult.ok(taskService.acceptTask(user, taskNo));
  }

  @PostMapping("/orders/{orderNo}/submit")
  public ApiResult<Void> submit(@PathVariable String orderNo, @Valid @RequestBody SubmitOrderRequest req) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    taskService.submitOrder(user, orderNo, req);
    return ApiResult.ok(null);
  }

  @GetMapping("/orders/my")
  public ApiResult<SliceResponse<OrderListItem>> myOrders(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    return ApiResult.ok(taskService.myOrdersSlice(user, page, size));
  }

  private com.leiyang.anontask.domain.UserAccount resolveOptionalViewer() {
    try {
      long userId = SecurityUtil.requireMpUserId();
      return userService.requireUser(userId);
    } catch (Exception e) {
      return null;
    }
  }
}
