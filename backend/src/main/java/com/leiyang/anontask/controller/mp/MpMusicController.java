package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.AiMusicService;
import com.leiyang.anontask.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mp/music")
public class MpMusicController {
  private final AiMusicService service;
  private final UserService userService;

  public MpMusicController(AiMusicService service, UserService userService) {
    this.service = service;
    this.userService = userService;
  }

  public record GenerateReq(
      @NotBlank(message = "title is required") @Size(max = 100, message = "title too long") String title,
      @NotBlank(message = "prompt is required") @Size(max = 5000, message = "prompt too long") String prompt,
      @Size(max = 1000, message = "style too long") String style,
      Boolean customMode,
      Boolean custom_mode,
      Boolean instrumental,
      @Size(max = 32, message = "lang too long") String lang
  ) {}

  public record AssistReq(
      @NotBlank(message = "type is required") String type,
      @Size(max = 100, message = "title too long") String title,
      @Size(max = 5000, message = "prompt too long") String prompt,
      @Size(max = 1000, message = "style too long") String style,
      @Size(max = 32, message = "lang too long") String lang
  ) {}

  public record AssistResp(String text) {}

  @GetMapping("")
  public ApiResult<List<AiMusicService.MusicItem>> list() {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(service.list(userService.requireUser(userId)));
  }

  @PostMapping("/generate")
  public ApiResult<AiMusicService.MusicItem> generate(@Valid @RequestBody GenerateReq req) {
    long userId = SecurityUtil.requireMpUserId();
    boolean custom = req.customMode() != null ? req.customMode() : Boolean.TRUE.equals(req.custom_mode());
    return ApiResult.ok(service.create(
        userService.requireUser(userId),
        req.title(),
        req.prompt(),
        req.style(),
        custom,
        Boolean.TRUE.equals(req.instrumental()),
        req.lang()
    ));
  }

  @PostMapping("/assist")
  public ApiResult<AssistResp> assist(@Valid @RequestBody AssistReq req) {
    SecurityUtil.requireMpUserId();
    return ApiResult.ok(new AssistResp(service.assist(req.type(), req.title(), req.prompt(), req.style(), req.lang())));
  }

  @PostMapping("/{id}/refresh")
  public ApiResult<AiMusicService.MusicItem> refresh(@PathVariable long id) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(service.refresh(userService.requireUser(userId), id));
  }
}
