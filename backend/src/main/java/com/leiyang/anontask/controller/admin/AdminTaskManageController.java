package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.TaskOrder;
import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.enums.TaskStatus;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.dto.admin.TaskAdminDetail;
import com.leiyang.anontask.dto.admin.TaskAdminListItem;
import com.leiyang.anontask.repo.TaskOrderRepository;
import com.leiyang.anontask.repo.TaskPublishRepository;
import com.leiyang.anontask.repo.TaskSubmitProofRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/task-manage")
public class AdminTaskManageController {
  private final TaskPublishRepository repo;
  private final TaskOrderRepository orderRepo;
  private final TaskSubmitProofRepository proofRepo;

  public AdminTaskManageController(TaskPublishRepository repo, TaskOrderRepository orderRepo, TaskSubmitProofRepository proofRepo) {
    this.repo = repo;
    this.orderRepo = orderRepo;
    this.proofRepo = proofRepo;
  }

  public record TaskSubmissionProof(String type, String url, String remark, String createdAt) {}
  public record TaskSubmissionDetail(
      String orderNo,
      String orderStatus,
      String auditReason,
      String acceptUserOpenId,
      String acceptUserName,
      String acceptTime,
      String submitTime,
      String settledTime,
      String settledAmount,
      List<TaskSubmissionProof> proofs
  ) {}

  @GetMapping("/tasks")
  public ApiResult<PageResponse<TaskAdminListItem>> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    TaskStatus st = null;
    if (status != null && !status.isBlank()) {
      st = TaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }
    int safePage = Math.max(1, page);
    int safeSize = Math.min(100, Math.max(5, size));
    var p = repo.adminSearch(st, q == null || q.isBlank() ? null : q.trim(),
        PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    var items = p.getContent().stream()
        .map(t -> new TaskAdminListItem(
            t.getTaskNo(),
            t.getStatus().name(),
            t.getTitle(),
            t.getAmount(),
            t.getTotalSlots(),
            t.getAcceptedSlots(),
            t.getDeadlineAt().toString(),
            t.getPublisher().getOpenId(),
            t.getCreatedAt().toString()
        ))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), items));
  }

  @GetMapping("/tasks/{taskNo}")
  public ApiResult<TaskAdminDetail> detail(@PathVariable String taskNo) {
    TaskPublish t = repo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    return ApiResult.ok(new TaskAdminDetail(
        t.getTaskNo(),
        t.getStatus().name(),
        t.getTitle(),
        t.getContent(),
        t.getLocationText(),
        t.getAmount(),
        t.getTotalSlots(),
        t.getAcceptedSlots(),
        t.getDeadlineAt().toString(),
        t.getProofRequirements(),
        t.getRejectReason(),
        t.getPublisher().getOpenId(),
        t.getCreatedAt().toString()
    ));
  }

  @GetMapping("/tasks/{taskNo}/submissions")
  public ApiResult<List<TaskSubmissionDetail>> submissions(@PathVariable String taskNo) {
    TaskPublish t = repo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    return ApiResult.ok(orderRepo.findByTaskAndSubmitTimeIsNotNullOrderBySubmitTimeDesc(t).stream()
        .map(this::toSubmissionDetail)
        .toList());
  }

  private TaskSubmissionDetail toSubmissionDetail(TaskOrder o) {
    var proofs = proofRepo.findByOrderOrderByIdAsc(o).stream()
        .map(p -> new TaskSubmissionProof(
            p.getProofType(),
            p.getProofUrl(),
            p.getRemark(),
            p.getCreatedAt() == null ? "" : p.getCreatedAt().toString()
        ))
        .toList();
    return new TaskSubmissionDetail(
        o.getOrderNo(),
        o.getOrderStatus().name(),
        o.getAuditReason(),
        o.getAcceptUser().getOpenId(),
        displayName(o.getAcceptUser().getNickname(), o.getAcceptUser().getOpenId()),
        o.getAcceptTime() == null ? "" : o.getAcceptTime().toString(),
        o.getSubmitTime() == null ? "" : o.getSubmitTime().toString(),
        o.getSettledTime() == null ? "" : o.getSettledTime().toString(),
        o.getSettledAmount() == null ? "" : o.getSettledAmount().toPlainString(),
        proofs
    );
  }

  public record CloseReq(@NotBlank(message = "reason is required") String reason) {}

  @PostMapping("/tasks/{taskNo}/close")
  @Transactional
  public ApiResult<Void> close(@PathVariable String taskNo, @Valid @RequestBody CloseReq req) {
    TaskPublish t = repo.findByTaskNo(taskNo).orElseThrow(() -> new BizException("Task not found"));
    if (t.getStatus() == TaskStatus.CLOSED) {
      return ApiResult.ok(null);
    }
    t.setStatus(TaskStatus.CLOSED);
    t.setRejectReason(req.reason());
    repo.save(t);
    return ApiResult.ok(null);
  }

  private static String displayName(String nickname, String openId) {
    String n = trim(nickname);
    if (!n.isEmpty()) return n;
    String open = trim(openId);
    if (open.length() > 6) return "用户" + open.substring(open.length() - 6);
    return open.isEmpty() ? "用户" : open;
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
