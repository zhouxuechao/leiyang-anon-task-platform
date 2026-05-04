package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.ReportRecord;
import com.leiyang.anontask.domain.enums.ReportStatus;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.ReportRecordRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/admin/reports")
public class AdminReportController {
  private final ReportRecordRepository repo;

  public AdminReportController(ReportRecordRepository repo) {
    this.repo = repo;
  }

  public record ReportItem(String reportNo, String reporterOpenId, String targetType, long targetId, String reason, String status, String createdAt) {}

  public record ResolveReq(@NotBlank(message = "result is required") String result) {}

  @GetMapping("/pending")
  public ApiResult<PageResponse<ReportItem>> pending(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    var p = repo.findByStatus(ReportStatus.PENDING, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    var list = p.getContent().stream()
        .map(r -> new ReportItem(
            r.getReportNo(),
            r.getReporter().getOpenId(),
            r.getTargetType(),
            r.getTargetId(),
            r.getReason(),
            r.getStatus().name(),
            r.getCreatedAt().toString()
        ))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), list));
  }

  @PostMapping("/{reportNo}/resolve")
  @Transactional
  public ApiResult<Void> resolve(@PathVariable String reportNo, @Valid @RequestBody ResolveReq req) {
    ReportRecord r = repo.findByReportNo(reportNo).orElseThrow(() -> new BizException("Report not found"));
    if (r.getStatus() != ReportStatus.PENDING) {
      throw new BizException("Report is not pending");
    }
    if ("RESOLVE".equalsIgnoreCase(req.result())) {
      r.setStatus(ReportStatus.RESOLVED);
    } else if ("REJECT".equalsIgnoreCase(req.result())) {
      r.setStatus(ReportStatus.REJECTED);
    } else {
      throw new BizException("Invalid result");
    }
    repo.save(r);
    return ApiResult.ok(null);
  }
}
