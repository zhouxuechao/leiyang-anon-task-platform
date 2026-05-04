package com.leiyang.anontask.controller.mini;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.mp.SliceResponse;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.AiMusicService;
import com.leiyang.anontask.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini/music")
public class MiniMusicController {
  private final AiMusicService musicService;
  private final UserService userService;

  public MiniMusicController(AiMusicService musicService, UserService userService) {
    this.musicService = musicService;
    this.userService = userService;
  }

  public record GenerateReq(
      @NotBlank(message = "prompt is required") @Size(max = 5000, message = "prompt too long") String prompt,
      @Size(max = 100, message = "title too long") String title,
      @Size(max = 1000, message = "style too long") String style,
      Boolean custom_mode,
      Boolean instrumental,
      @Size(max = 32, message = "lang too long") String lang
  ) {}

  public record GenerateResp(long generation_id) {}

  public record LyricsReq(
      @NotBlank(message = "theme is required") @Size(max = 500, message = "theme too long") String theme,
      @Size(max = 100, message = "title too long") String title,
      @Size(max = 5000, message = "previous lyrics too long") String previousLyrics,
      @Size(max = 1000, message = "style too long") String style
  ) {}

  public record LyricsResp(String lyrics) {}

  public record RateReq(@Min(1) @Max(5) int stars) {}

  public record TipReq(@DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount) {}
  public record CommentReq(@NotBlank(message = "content is required") @Size(max = 512, message = "content too long") String content) {}
  public record CommentResp(long commentId) {}
  public record RenameReq(@NotBlank(message = "title is required") @Size(max = 100, message = "title too long") String title) {}

  public record BuyPackageResp(String code, String name, int credits, BigDecimal price) {}

  @GetMapping("/credits")
  public ApiResult<AiMusicService.CreditInfo> credits() {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.credits(userService.requireUser(userId)));
  }

  @GetMapping("/packages")
  public ApiResult<List<AiMusicService.MusicPackage>> packages() {
    return ApiResult.ok(musicService.packages());
  }

  @PostMapping("/packages/{code}/buy")
  public ApiResult<BuyPackageResp> buyPackage(@PathVariable String code) {
    long userId = SecurityUtil.requireMpUserId();
    var p = musicService.buyPackage(userService.requireUser(userId), code);
    return ApiResult.ok(new BuyPackageResp(p.code(), p.name(), p.credits(), p.price()));
  }

  @PostMapping("/generate")
  public ApiResult<GenerateResp> generate(@Valid @RequestBody GenerateReq req) {
    long userId = SecurityUtil.requireMpUserId();
    String title = req.title() == null || req.title().trim().isEmpty() ? "AI歌曲" : req.title();
    boolean customMode = req.custom_mode() == null || req.custom_mode();
    String lang = req.lang() == null || req.lang().trim().isEmpty() ? "zh" : req.lang().trim();
    var item = musicService.create(userService.requireUser(userId), title, req.prompt(), req.style(), customMode, Boolean.TRUE.equals(req.instrumental()), lang);
    return ApiResult.ok(new GenerateResp(item.id()));
  }

  @GetMapping("/generation/{id}/status")
  public ApiResult<AiMusicService.GenerationStatus> generationStatus(@PathVariable long id) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.generationStatus(userService.requireUser(userId), id));
  }

  @PostMapping("/generate-lyrics")
  public ApiResult<LyricsResp> generateLyrics(@Valid @RequestBody LyricsReq req) {
    SecurityUtil.requireMpUserId();
    return ApiResult.ok(new LyricsResp(musicService.generateLyrics(req.title(), req.theme(), req.style(), req.previousLyrics())));
  }

  @GetMapping("/my-songs")
  public ApiResult<List<AiMusicService.MusicItem>> mySongs() {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.list(userService.requireUser(userId)));
  }

  @GetMapping("/song/{id}")
  public ApiResult<AiMusicService.MusicItem> song(@PathVariable long id) {
    Long userId = optionalUserId();
    if (userId == null) return ApiResult.ok(musicService.detail(id));
    return ApiResult.ok(musicService.detail(userService.requireUser(userId), id));
  }

  @GetMapping("/song/{id}/comments")
  public ApiResult<List<AiMusicService.MusicCommentItem>> comments(@PathVariable long id) {
    return ApiResult.ok(musicService.comments(id));
  }

  @PostMapping("/song/{id}/comments")
  public ApiResult<CommentResp> comment(@PathVariable long id, @Valid @RequestBody CommentReq req) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(new CommentResp(musicService.addComment(userService.requireUser(userId), id, req.content())));
  }

  @PostMapping("/song/{id}/publish")
  public ApiResult<AiMusicService.MusicItem> publish(@PathVariable long id) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.publish(userService.requireUser(userId), id));
  }

  @PatchMapping("/song/{id}")
  public ApiResult<AiMusicService.MusicItem> rename(@PathVariable long id, @Valid @RequestBody RenameReq req) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.rename(userService.requireUser(userId), id, req.title()));
  }

  @DeleteMapping("/song/{id}")
  public ApiResult<Void> delete(@PathVariable long id) {
    long userId = SecurityUtil.requireMpUserId();
    musicService.delete(userService.requireUser(userId), id);
    return ApiResult.ok(null);
  }

  @PostMapping("/song/{id}/share-circle")
  public ApiResult<AiMusicService.MusicItem> shareCircle(@PathVariable long id) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.shareCircle(userService.requireUser(userId), id));
  }

  @PostMapping("/song/{id}/rate")
  public ApiResult<AiMusicService.MusicItem> rate(@PathVariable long id, @Valid @RequestBody RateReq req) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.rate(userService.requireUser(userId), id, req.stars()));
  }

  @PostMapping("/song/{id}/tip")
  public ApiResult<AiMusicService.MusicItem> tip(@PathVariable long id, @Valid @RequestBody TipReq req) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(musicService.tip(userService.requireUser(userId), id, req.amount()));
  }

  @GetMapping("/hall")
  public ApiResult<SliceResponse<AiMusicService.MusicItem>> hall(
      @RequestParam(defaultValue = "hot") String sort,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Long userId = optionalUserId();
    if (userId == null) return ApiResult.ok(musicService.hallSlice(sort, page, size));
    return ApiResult.ok(musicService.hallSlice(userService.requireUser(userId), sort, page, size));
  }

  private Long optionalUserId() {
    try {
      return SecurityUtil.requireMpUserId();
    } catch (Exception e) {
      return null;
    }
  }
}
