package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.PlazaAiProvider;
import com.leiyang.anontask.domain.PlazaCategory;
import com.leiyang.anontask.domain.PlazaSortOption;
import com.leiyang.anontask.domain.enums.GeneralStatus;
import com.leiyang.anontask.repo.PlazaAiProviderRepository;
import com.leiyang.anontask.repo.PlazaCategoryRepository;
import com.leiyang.anontask.repo.PlazaSortOptionRepository;
import com.leiyang.anontask.repo.UserAccountRepository;
import com.leiyang.anontask.service.RedisSupportService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/plaza")
public class AdminPlazaConfigController {
  private final PlazaCategoryRepository categoryRepo;
  private final PlazaAiProviderRepository aiProviderRepo;
  private final PlazaSortOptionRepository sortOptionRepo;
  private final UserAccountRepository userRepo;
  private final RedisSupportService redisSupport;

  public AdminPlazaConfigController(
      PlazaCategoryRepository categoryRepo,
      PlazaAiProviderRepository aiProviderRepo,
      PlazaSortOptionRepository sortOptionRepo,
      UserAccountRepository userRepo,
      RedisSupportService redisSupport
  ) {
    this.categoryRepo = categoryRepo;
    this.aiProviderRepo = aiProviderRepo;
    this.sortOptionRepo = sortOptionRepo;
    this.userRepo = userRepo;
    this.redisSupport = redisSupport;
  }

  public record CategoryItem(long id, String code, String name, String keywords, String status, int sortNo) {}
  public record CategoryReq(
      Long id,
      @NotBlank(message = "code is required") String code,
      @NotBlank(message = "name is required") String name,
      String keywords,
      Integer sortNo
  ) {}

  public record AiProviderItem(
      long id,
      String code,
      String name,
      String abbr,
      String logoText,
      String logoUrl,
      String status,
      int sortNo,
      String baseUrl,
      String model,
      String apiKey,
      Integer timeoutMs,
      Double temperature
  ) {}
  public record AiProviderReq(
      Long id,
      @NotBlank(message = "code is required") String code,
      @NotBlank(message = "name is required") String name,
      @NotBlank(message = "abbr is required") String abbr,
      String logoText,
      String logoUrl,
      Integer sortNo,
      String baseUrl,
      String model,
      String apiKey,
      Integer timeoutMs,
      Double temperature
  ) {}

  public record SortItem(long id, String code, String name, String status, int sortNo) {}
  public record SortReq(
      Long id,
      @NotBlank(message = "code is required") String code,
      @NotBlank(message = "name is required") String name,
      Integer sortNo
  ) {}

  @GetMapping("/categories")
  public ApiResult<List<CategoryItem>> listCategories() {
    var list = categoryRepo.findAll().stream()
        .sorted((a, b) -> {
          if (a.getSortNo() != b.getSortNo()) return Integer.compare(a.getSortNo(), b.getSortNo());
          return Long.compare(a.getId(), b.getId());
        })
        .map(c -> new CategoryItem(c.getId(), c.getCode(), c.getName(), c.getKeywords(), c.getStatus(), c.getSortNo()))
        .toList();
    return ApiResult.ok(list);
  }

  @PostMapping("/categories")
  @Transactional
  public ApiResult<Void> upsertCategory(@Valid @RequestBody CategoryReq req) {
    PlazaCategory c = req.id() == null ? new PlazaCategory() : categoryRepo.findById(req.id()).orElseThrow(() -> new BizException("Category not found"));
    c.setCode(normCode(req.code()));
    c.setName(req.name().trim());
    c.setKeywords(req.keywords());
    c.setSortNo(req.sortNo() == null ? 0 : req.sortNo());
    if (c.getStatus() == null || c.getStatus().isBlank()) c.setStatus(GeneralStatus.ACTIVE.name());
    categoryRepo.save(c);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @PostMapping("/categories/{id}/toggle")
  @Transactional
  public ApiResult<Void> toggleCategory(@PathVariable long id) {
    PlazaCategory c = categoryRepo.findById(id).orElseThrow(() -> new BizException("Category not found"));
    c.setStatus(GeneralStatus.ACTIVE.name().equals(c.getStatus()) ? GeneralStatus.INACTIVE.name() : GeneralStatus.ACTIVE.name());
    categoryRepo.save(c);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @PostMapping("/categories/{id}/delete")
  @Transactional
  public ApiResult<Void> deleteCategory(@PathVariable long id) {
    if (!categoryRepo.existsById(id)) throw new BizException("Category not found");
    categoryRepo.deleteById(id);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @GetMapping("/ai-providers")
  public ApiResult<List<AiProviderItem>> listAiProviders() {
    var list = aiProviderRepo.findAll().stream()
        .sorted((a, b) -> {
          if (a.getSortNo() != b.getSortNo()) return Integer.compare(a.getSortNo(), b.getSortNo());
          return Long.compare(a.getId(), b.getId());
        })
        .map(a -> new AiProviderItem(
            a.getId(),
            a.getCode(),
            a.getName(),
            a.getAbbr(),
            a.getLogoText(),
            a.getLogoUrl(),
            a.getStatus(),
            a.getSortNo(),
            a.getBaseUrl(),
            a.getModel(),
            mask(a.getApiKey()),
            a.getTimeoutMs(),
            a.getTemperature()
        ))
        .toList();
    return ApiResult.ok(list);
  }

  @PostMapping("/ai-providers")
  @Transactional
  public ApiResult<Void> upsertAiProvider(@Valid @RequestBody AiProviderReq req) {
    PlazaAiProvider a = req.id() == null ? new PlazaAiProvider() : aiProviderRepo.findById(req.id()).orElseThrow(() -> new BizException("AI provider not found"));
    a.setCode(req.code().trim());
    a.setName(req.name().trim());
    a.setAbbr(req.abbr().trim());
    a.setLogoText(req.logoText());
    a.setLogoUrl(trimToNull(req.logoUrl()));
    a.setSortNo(req.sortNo() == null ? 0 : req.sortNo());
    a.setBaseUrl(trimToNull(req.baseUrl()));
    a.setModel(trimToNull(req.model()));
    if (trimToNull(req.apiKey()) != null && !isMasked(req.apiKey())) {
      a.setApiKey(trimToNull(req.apiKey()));
    }
    a.setTimeoutMs(req.timeoutMs());
    a.setTemperature(req.temperature());
    if (a.getStatus() == null || a.getStatus().isBlank()) a.setStatus(GeneralStatus.ACTIVE.name());
    PlazaAiProvider saved = aiProviderRepo.save(a);
    syncAiBotLogo(saved);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @PostMapping("/ai-providers/{id}/toggle")
  @Transactional
  public ApiResult<Void> toggleAiProvider(@PathVariable long id) {
    PlazaAiProvider a = aiProviderRepo.findById(id).orElseThrow(() -> new BizException("AI provider not found"));
    a.setStatus(GeneralStatus.ACTIVE.name().equals(a.getStatus()) ? GeneralStatus.INACTIVE.name() : GeneralStatus.ACTIVE.name());
    aiProviderRepo.save(a);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @PostMapping("/ai-providers/{id}/delete")
  @Transactional
  public ApiResult<Void> deleteAiProvider(@PathVariable long id) {
    if (!aiProviderRepo.existsById(id)) throw new BizException("AI provider not found");
    aiProviderRepo.deleteById(id);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @GetMapping("/sort-options")
  public ApiResult<List<SortItem>> listSortOptions() {
    var list = sortOptionRepo.findAll().stream()
        .sorted((a, b) -> {
          if (a.getSortNo() != b.getSortNo()) return Integer.compare(a.getSortNo(), b.getSortNo());
          return Long.compare(a.getId(), b.getId());
        })
        .map(s -> new SortItem(s.getId(), s.getCode(), s.getName(), s.getStatus(), s.getSortNo()))
        .toList();
    return ApiResult.ok(list);
  }

  @PostMapping("/sort-options")
  @Transactional
  public ApiResult<Void> upsertSortOption(@Valid @RequestBody SortReq req) {
    PlazaSortOption s = req.id() == null ? new PlazaSortOption() : sortOptionRepo.findById(req.id()).orElseThrow(() -> new BizException("Sort option not found"));
    s.setCode(normCode(req.code()));
    s.setName(req.name().trim());
    s.setSortNo(req.sortNo() == null ? 0 : req.sortNo());
    if (s.getStatus() == null || s.getStatus().isBlank()) s.setStatus(GeneralStatus.ACTIVE.name());
    sortOptionRepo.save(s);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @PostMapping("/sort-options/{id}/toggle")
  @Transactional
  public ApiResult<Void> toggleSortOption(@PathVariable long id) {
    PlazaSortOption s = sortOptionRepo.findById(id).orElseThrow(() -> new BizException("Sort option not found"));
    s.setStatus(GeneralStatus.ACTIVE.name().equals(s.getStatus()) ? GeneralStatus.INACTIVE.name() : GeneralStatus.ACTIVE.name());
    sortOptionRepo.save(s);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  @PostMapping("/sort-options/{id}/delete")
  @Transactional
  public ApiResult<Void> deleteSortOption(@PathVariable long id) {
    if (!sortOptionRepo.existsById(id)) throw new BizException("Sort option not found");
    sortOptionRepo.deleteById(id);
    clearPlazaMetaCache();
    return ApiResult.ok(null);
  }

  private void clearPlazaMetaCache() {
    redisSupport.delete("cache:mp:plaza:meta:v1");
  }

  private static boolean isMasked(String value) {
    return value != null && value.contains("***");
  }

  private static String mask(String value) {
    String v = trimToNull(value);
    if (v == null) return null;
    if (v.length() <= 8) return "****";
    return v.substring(0, 3) + "***" + v.substring(v.length() - 3);
  }

  private static String normCode(String value) {
    return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
  }

  private static String trimToNull(String value) {
    if (value == null) return null;
    String v = value.trim();
    return v.isEmpty() ? null : v;
  }

  private void syncAiBotLogo(PlazaAiProvider provider) {
    String code = normCode(provider.getCode()).toLowerCase(Locale.ROOT);
    String logoUrl = trimToNull(provider.getLogoUrl());
    userRepo.findByOpenId("ai-bot:" + code).ifPresent(u -> {
      String current = trimToNull(u.getAvatar());
      if (logoUrl == null ? current != null : !logoUrl.equals(current)) {
        u.setAvatar(logoUrl);
      }
      String nick = provider.getName() + " AI";
      if (!nick.equals(trimToEmpty(u.getNickname()))) u.setNickname(nick);
      userRepo.save(u);
    });
  }

  private static String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }
}
