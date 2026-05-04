package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.domain.enums.TaskStatus;
import com.leiyang.anontask.dto.admin.AuditRequest;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.TaskPublishRepository;
import com.leiyang.anontask.service.AdminAuditService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {
  private final TaskPublishRepository taskRepo;
  private final AdminAuditService auditService;

  public AdminTaskController(TaskPublishRepository taskRepo, AdminAuditService auditService) {
    this.taskRepo = taskRepo;
    this.auditService = auditService;
  }

  public record TaskPendingItem(String taskNo, String title, String amount, String deadlineAt, String publisherOpenId) {}

  @GetMapping("/pending")
  public ApiResult<PageResponse<TaskPendingItem>> pending(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    var p = taskRepo.adminSearch(TaskStatus.PENDING_AUDIT, null, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    var list = p.getContent().stream()
        .map(t -> new TaskPendingItem(
            t.getTaskNo(),
            t.getTitle(),
            t.getAmount().toPlainString(),
            t.getDeadlineAt().toString(),
            t.getPublisher().getOpenId()
        ))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), list));
  }

  @PostMapping("/{taskNo}/audit")
  public ApiResult<Void> audit(@PathVariable String taskNo, @Valid @RequestBody AuditRequest req) {
    auditService.auditTask(taskNo, req.result(), req.reason());
    return ApiResult.ok(null);
  }
}
