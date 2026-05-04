package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.SysBanner;
import com.leiyang.anontask.repo.SysBannerRepository;
import com.leiyang.anontask.service.RedisSupportService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {
  private final SysBannerRepository repo;
  private final RedisSupportService redisSupport;

  public AdminBannerController(SysBannerRepository repo, RedisSupportService redisSupport) {
    this.repo = repo;
    this.redisSupport = redisSupport;
  }

  public record BannerItem(long id, String imageUrl, String linkUrl, String status, int sortNo, String createdAt) {}
  public record UpsertReq(
      Long id,
      @NotBlank(message = "imageUrl is required") String imageUrl,
      String linkUrl,
      @NotNull(message = "status is required") String status,
      int sortNo
  ) {}

  @GetMapping("")
  public ApiResult<List<BannerItem>> list() {
    var list = repo.findAll().stream()
        .map(b -> new BannerItem(b.getId(), b.getImageUrl(), b.getLinkUrl(), b.getStatus(), b.getSortNo(), b.getCreatedAt().toString()))
        .toList();
    return ApiResult.ok(list);
  }

  @PostMapping("")
  @Transactional
  public ApiResult<Void> upsert(@Valid @RequestBody UpsertReq req) {
    SysBanner b = req.id() == null ? new SysBanner() : repo.findById(req.id()).orElseThrow(() -> new BizException("Banner not found"));
    b.setImageUrl(req.imageUrl());
    b.setLinkUrl(req.linkUrl());
    b.setStatus(req.status());
    b.setSortNo(req.sortNo());
    if (b.getCreatedAt() == null) b.setCreatedAt(Instant.now());
    repo.save(b);
    redisSupport.delete("cache:mp:home:v1");
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/delete")
  @Transactional
  public ApiResult<Void> delete(@PathVariable long id) {
    if (!repo.existsById(id)) throw new BizException("Banner not found");
    repo.deleteById(id);
    redisSupport.delete("cache:mp:home:v1");
    return ApiResult.ok(null);
  }
}
