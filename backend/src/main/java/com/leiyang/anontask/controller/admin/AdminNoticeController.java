package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.SysNotice;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.SysNoticeRepository;
import com.leiyang.anontask.service.RedisSupportService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {
  private final SysNoticeRepository repo;
  private final RedisSupportService redisSupport;

  public AdminNoticeController(SysNoticeRepository repo, RedisSupportService redisSupport) {
    this.repo = repo;
    this.redisSupport = redisSupport;
  }

  public record NoticeItem(long id, String title, String content, String status, int sortNo, String createdAt) {}
  public record UpsertReq(
      Long id,
      @NotBlank(message = "title is required") String title,
      @NotBlank(message = "content is required") String content,
      @NotNull(message = "status is required") String status,
      int sortNo
  ) {}

  @GetMapping("")
  public ApiResult<PageResponse<NoticeItem>> list(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    var p = repo.findAll(PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.asc("sortNo"), Sort.Order.desc("createdAt"))));
    var list = p.getContent().stream()
        .map(n -> new NoticeItem(n.getId(), n.getTitle(), n.getContent(), n.getStatus(), n.getSortNo(), n.getCreatedAt().toString()))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), list));
  }

  @PostMapping("")
  @Transactional
  public ApiResult<Void> upsert(@Valid @RequestBody UpsertReq req) {
    SysNotice n = req.id() == null ? new SysNotice() : repo.findById(req.id()).orElseThrow(() -> new BizException("Notice not found"));
    n.setTitle(req.title());
    n.setContent(req.content());
    n.setStatus(req.status());
    n.setSortNo(req.sortNo());
    if (n.getCreatedAt() == null) n.setCreatedAt(Instant.now());
    repo.save(n);
    redisSupport.delete("cache:mp:home:v1");
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/delete")
  @Transactional
  public ApiResult<Void> delete(@PathVariable long id) {
    if (!repo.existsById(id)) throw new BizException("Notice not found");
    repo.deleteById(id);
    redisSupport.delete("cache:mp:home:v1");
    return ApiResult.ok(null);
  }
}
