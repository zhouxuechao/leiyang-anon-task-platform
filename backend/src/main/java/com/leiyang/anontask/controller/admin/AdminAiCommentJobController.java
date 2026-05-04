package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.AiCommentJobRepository;
import com.leiyang.anontask.service.AiAutomationService;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-comment-jobs")
public class AdminAiCommentJobController {
  private final AiCommentJobRepository repo;
  private final AiAutomationService aiAutomationService;

  public AdminAiCommentJobController(AiCommentJobRepository repo, AiAutomationService aiAutomationService) {
    this.repo = repo;
    this.aiAutomationService = aiAutomationService;
  }

  public record JobItem(
      long id,
      long postId,
      String providerCode,
      String status,
      int attempts,
      Instant nextRetryAt,
      String lastError,
      Long commentId,
      Instant processedAt,
      Instant createdAt
  ) {}

  public record Summary(long pending, long success, long failed) {}
  public record ConsumeResp(int processed) {}

  @GetMapping("")
  public ApiResult<PageResponse<JobItem>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "") String q
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(100, Math.max(1, size));
    String st = trim(status).toUpperCase();
    String query = trim(q);
    var pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    var p = repo.adminSearch(st, query, pageable);
    var items = p.getContent().stream().map(v -> new JobItem(
        v.getId(),
        v.getPost().getId(),
        v.getProviderCode(),
        v.getStatus(),
        v.getAttempts(),
        v.getNextRetryAt(),
        v.getLastError(),
        v.getCommentId(),
        v.getProcessedAt(),
        v.getCreatedAt()
    )).toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), items));
  }

  @GetMapping("/summary")
  public ApiResult<Summary> summary() {
    long pending = repo.countByStatus("PENDING");
    long success = repo.countByStatus("SUCCESS");
    long failed = repo.countByStatus("FAILED");
    return ApiResult.ok(new Summary(pending, success, failed));
  }

  @PostMapping("/consume-once")
  public ApiResult<ConsumeResp> consumeOnce() {
    int processed = aiAutomationService.triggerCommentConsumeOnce();
    return ApiResult.ok(new ConsumeResp(processed));
  }

  @PostMapping("/{id}/retry")
  public ApiResult<Void> retry(@PathVariable long id) {
    aiAutomationService.retryCommentJob(id);
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/delete")
  public ApiResult<Void> delete(@PathVariable long id) {
    aiAutomationService.deleteCommentJob(id);
    return ApiResult.ok(null);
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
