package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.UserFeedbackRepository;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedbacks")
public class AdminFeedbackController {
  private final UserFeedbackRepository feedbackRepo;

  public AdminFeedbackController(UserFeedbackRepository feedbackRepo) {
    this.feedbackRepo = feedbackRepo;
  }

  public record FeedbackItem(
      long id,
      long userId,
      String openId,
      String nickname,
      String content,
      String contact,
      String status,
      Instant createdAt
  ) {}

  @GetMapping("")
  public ApiResult<PageResponse<FeedbackItem>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String q
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(100, Math.max(1, size));
    String query = trim(q);
    var pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    var p = feedbackRepo.adminSearch(query, pageable);
    var items = p.getContent().stream().map(v -> new FeedbackItem(
        v.getId(),
        v.getUser().getId(),
        v.getUser().getOpenId(),
        trim(v.getUser().getNickname()),
        trim(v.getContent()),
        trim(v.getContact()),
        trim(v.getStatus()),
        v.getCreatedAt()
    )).toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), items));
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
