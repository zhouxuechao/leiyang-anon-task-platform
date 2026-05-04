package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.SysOpLogRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/op-logs")
public class AdminOpLogController {
  private final SysOpLogRepository repo;

  public AdminOpLogController(SysOpLogRepository repo) {
    this.repo = repo;
  }

  public record LogItem(
      long id,
      String actorType,
      long actorId,
      String method,
      String path,
      String ip,
      String userAgent,
      boolean ok,
      String error,
      String createdAt
  ) {}

  @GetMapping("")
  public ApiResult<PageResponse<LogItem>> list(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    var p = repo.findAll(PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    var list = p.getContent().stream()
        .map(l -> new LogItem(
            l.getId(),
            l.getActorType(),
            l.getActorId(),
            l.getMethod(),
            l.getPath(),
            l.getIp(),
            l.getUserAgent(),
            l.isOkFlag(),
            l.getErrorMsg(),
            l.getCreatedAt().toString()
        ))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), list));
  }
}
