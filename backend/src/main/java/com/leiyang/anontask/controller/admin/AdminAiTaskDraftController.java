package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.domain.AiTaskDraft;
import com.leiyang.anontask.service.AdminAiTaskDraftService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-task-drafts")
public class AdminAiTaskDraftController {
  private final AdminAiTaskDraftService service;

  public AdminAiTaskDraftController(AdminAiTaskDraftService service) {
    this.service = service;
  }

  public record DraftItem(
      Long id,
      String providerCode,
      String title,
      String content,
      String category,
      String locationText,
      String amount,
      Integer totalSlots,
      String deadlineAt,
      String proofRequirements,
      String status,
      String publishedTaskNo,
      String createdAt
  ) {}

  public record GenerateReq(String providerCode) {}

  public record UpdateReq(
      @NotBlank(message = "title is required") String title,
      @NotBlank(message = "content is required") String content,
      String category,
      String locationText,
      @DecimalMin(value = "1.00", message = "amount must be >= 1.00") BigDecimal amount,
      @Min(value = 1, message = "totalSlots must be >= 1") Integer totalSlots,
      Instant deadlineAt,
      String proofRequirements
  ) {}

  public record PublishResp(String taskNo) {}

  @GetMapping("")
  public ApiResult<List<DraftItem>> list() {
    return ApiResult.ok(service.listDrafts().stream().map(this::toItem).toList());
  }

  @GetMapping("/providers")
  public ApiResult<List<AdminAiTaskDraftService.ProviderItem>> providers() {
    return ApiResult.ok(service.listReadyProviders());
  }

  @GetMapping("/providers/check")
  public ApiResult<List<AdminAiTaskDraftService.ProviderHealth>> checkProviders() {
    return ApiResult.ok(service.checkProviders());
  }

  @PostMapping("/generate")
  public ApiResult<DraftItem> generate(@RequestBody(required = false) GenerateReq req) {
    String code = req == null ? "" : req.providerCode();
    return ApiResult.ok(toItem(service.generateDraft(code)));
  }

  @PostMapping("/{id}/update")
  public ApiResult<DraftItem> update(@PathVariable Long id, @Valid @RequestBody UpdateReq req) {
    return ApiResult.ok(toItem(service.updateDraft(
        id,
        req.title(),
        req.content(),
        req.category(),
        req.locationText(),
        req.amount(),
        req.totalSlots(),
        req.deadlineAt(),
        req.proofRequirements()
    )));
  }

  @PostMapping("/{id}/publish")
  public ApiResult<PublishResp> publish(@PathVariable Long id) {
    return ApiResult.ok(new PublishResp(service.publishDraft(id)));
  }

  @PostMapping("/{id}/delete")
  public ApiResult<Void> delete(@PathVariable Long id) {
    service.deleteDraft(id);
    return ApiResult.ok(null);
  }

  private DraftItem toItem(AiTaskDraft d) {
    return new DraftItem(
        d.getId(),
        d.getProviderCode(),
        d.getTitle(),
        d.getContent(),
        d.getCategory(),
        d.getLocationText(),
        d.getAmount().toPlainString(),
        d.getTotalSlots(),
        d.getDeadlineAt() == null ? "" : d.getDeadlineAt().toString(),
        d.getProofRequirements(),
        d.getStatus(),
        d.getPublishedTaskNo() == null ? "" : d.getPublishedTaskNo(),
        d.getCreatedAt() == null ? "" : d.getCreatedAt().toString()
    );
  }
}
