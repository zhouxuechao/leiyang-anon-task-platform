package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.UserFeedback;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.UserService;
import com.leiyang.anontask.repo.UserFeedbackRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mp")
public class MpFeedbackController {
  private final UserFeedbackRepository feedbackRepo;
  private final UserService userService;

  public MpFeedbackController(UserFeedbackRepository feedbackRepo, UserService userService) {
    this.feedbackRepo = feedbackRepo;
    this.userService = userService;
  }

  public record SubmitReq(
      @NotBlank(message = "content is required")
      @Size(max = 1000, message = "content too long")
      String content,
      @Size(max = 128, message = "contact too long")
      String contact
  ) {}

  public record SubmitResp(long id) {}

  @PostMapping("/feedbacks")
  public ApiResult<SubmitResp> submit(@Valid @RequestBody SubmitReq req) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    String content = trim(req.content());
    if (content.isBlank()) throw new BizException("content is required");
    UserFeedback f = new UserFeedback();
    f.setUser(user);
    f.setContent(content);
    f.setContact(trim(req.contact()));
    f.setStatus("NEW");
    feedbackRepo.save(f);
    return ApiResult.ok(new SubmitResp(f.getId()));
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
