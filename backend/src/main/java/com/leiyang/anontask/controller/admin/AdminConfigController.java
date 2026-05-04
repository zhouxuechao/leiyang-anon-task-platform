package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.SysConfig;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.SysConfigRepository;
import com.leiyang.anontask.service.RedisSupportService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/configs")
public class AdminConfigController {
  private final SysConfigRepository repo;
  private final RedisSupportService redisSupport;

  public AdminConfigController(SysConfigRepository repo, RedisSupportService redisSupport) {
    this.repo = repo;
    this.redisSupport = redisSupport;
  }

  public record ConfigItem(long id, String key, String value, String remark, String updatedAt) {}
  public record UpsertReq(
      @NotBlank(message = "key is required") String key,
      @NotBlank(message = "value is required") String value,
      String remark
  ) {}

  @GetMapping("")
  public ApiResult<PageResponse<ConfigItem>> list(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean excludeAiProvider
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    if (excludeAiProvider) {
      var rows = repo.findAll().stream()
          .filter(c -> !c.getCfgKey().startsWith("ai.provider."))
          .sorted(Comparator.comparing(SysConfig::getCfgKey))
          .toList();
      var list = rows.stream()
          .skip((long) (safePage - 1) * safeSize)
          .limit(safeSize)
          .map(c -> new ConfigItem(
              c.getId(),
              c.getCfgKey(),
              displayValue(c.getCfgKey(), c.getCfgValue()),
              c.getRemark(),
              c.getUpdatedAt().toString()
          ))
          .toList();
      return ApiResult.ok(new PageResponse<>(safePage, safeSize, rows.size(), list));
    }
    var p = repo.findAll(PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.ASC, "cfgKey")));
    var list = p.getContent().stream()
        .map(c -> new ConfigItem(
            c.getId(),
            c.getCfgKey(),
            displayValue(c.getCfgKey(), c.getCfgValue()),
            c.getRemark(),
            c.getUpdatedAt().toString()
        ))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), list));
  }

  @PostMapping("")
  @Transactional
  public ApiResult<Void> upsert(@Valid @RequestBody UpsertReq req) {
    SysConfig c = repo.findByCfgKey(req.key()).orElseGet(SysConfig::new);
    c.setCfgKey(req.key());
    if (!(isSensitiveKey(req.key()) && isMasked(req.value()))) {
      c.setCfgValue(req.value());
    }
    c.setRemark(req.remark());
    c.setUpdatedAt(Instant.now());
    repo.save(c);
    clearPublicCaches();
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/delete")
  @Transactional
  public ApiResult<Void> delete(@PathVariable long id) {
    if (!repo.existsById(id)) throw new BizException("Config not found");
    repo.deleteById(id);
    clearPublicCaches();
    return ApiResult.ok(null);
  }

  private void clearPublicCaches() {
    redisSupport.delete("cache:mp:home:v1");
    redisSupport.delete("cache:mp:plaza:meta:v1");
  }

  private static String displayValue(String key, String value) {
    return isSensitiveKey(key) ? mask(value) : value;
  }

  private static boolean isSensitiveKey(String key) {
    String k = key == null ? "" : key.toLowerCase(Locale.ROOT);
    return k.contains("password") || k.contains("secret") || k.contains("api_key") || k.endsWith(".key") || k.contains("token");
  }

  private static boolean isMasked(String value) {
    return value != null && value.contains("***");
  }

  private static String mask(String value) {
    if (value == null || value.isBlank()) return "";
    String v = value.trim();
    if (v.length() <= 8) return "****";
    return v.substring(0, 3) + "***" + v.substring(v.length() - 3);
  }
}
