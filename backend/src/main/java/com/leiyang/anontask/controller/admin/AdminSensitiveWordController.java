package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.SysSensitiveWord;
import com.leiyang.anontask.domain.enums.GeneralStatus;
import com.leiyang.anontask.domain.enums.SensitiveWordAction;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.SysSensitiveWordRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/admin/sensitive-words")
public class AdminSensitiveWordController {
  private final SysSensitiveWordRepository repo;

  public AdminSensitiveWordController(SysSensitiveWordRepository repo) {
    this.repo = repo;
  }

  public record WordItem(long id, String word, int level, String actionType, String status) {}

  public record CreateReq(
      @NotBlank(message = "word is required") String word,
      @Min(value = 1, message = "level must be >= 1") int level,
      @NotNull(message = "actionType is required") SensitiveWordAction actionType
  ) {}

  public record StatusReq(@NotNull(message = "status is required") GeneralStatus status) {}

  @GetMapping("")
  public ApiResult<PageResponse<WordItem>> list(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    var p = repo.findAll(PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    var list = p.getContent().stream()
        .map(w -> new WordItem(w.getId(), w.getWord(), w.getLevel(), w.getActionType().name(), w.getStatus().name()))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), list));
  }

  @PostMapping("")
  @Transactional
  public ApiResult<Void> create(@Valid @RequestBody CreateReq req) {
    SysSensitiveWord w = new SysSensitiveWord();
    w.setWord(req.word());
    w.setLevel(req.level());
    w.setActionType(req.actionType());
    w.setStatus(GeneralStatus.ACTIVE);
    w.setCreatedAt(Instant.now());
    try {
      repo.save(w);
    } catch (Exception e) {
      throw new BizException("Word already exists");
    }
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/status")
  @Transactional
  public ApiResult<Void> setStatus(@PathVariable long id, @Valid @RequestBody StatusReq req) {
    SysSensitiveWord w = repo.findById(id).orElseThrow(() -> new BizException("Word not found"));
    w.setStatus(req.status());
    repo.save(w);
    return ApiResult.ok(null);
  }
}
