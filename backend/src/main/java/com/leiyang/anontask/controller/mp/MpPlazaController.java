package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.dto.mp.PlazaCreateRequest;
import com.leiyang.anontask.dto.mp.PlazaPostItem;
import com.leiyang.anontask.dto.mp.SliceResponse;
import com.leiyang.anontask.dto.mp.TaskListItem;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.AiMusicService;
import com.leiyang.anontask.service.MpPlazaService;
import com.leiyang.anontask.service.MpTaskService;
import com.leiyang.anontask.service.RedisSupportService;
import com.leiyang.anontask.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mp")
public class MpPlazaController {
  private final MpPlazaService plazaService;
  private final MpTaskService taskService;
  private final AiMusicService musicService;
  private final UserService userService;
  private final RedisSupportService redisSupport;

  public MpPlazaController(MpPlazaService plazaService, MpTaskService taskService, AiMusicService musicService, UserService userService, RedisSupportService redisSupport) {
    this.plazaService = plazaService;
    this.taskService = taskService;
    this.musicService = musicService;
    this.userService = userService;
    this.redisSupport = redisSupport;
  }

  @GetMapping("/public/plaza/posts")
  public ApiResult<SliceResponse<PlazaPostItem>> posts(
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "ALL") String gender,
      @RequestParam(defaultValue = "ALL") String category,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Long viewerId = optionalViewerId();
    return ApiResult.ok(plazaService.listSlice(viewerId, sort, gender, category, page, size));
  }

  @GetMapping("/plaza/my-posts")
  public ApiResult<SliceResponse<PlazaPostItem>> myPosts(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    long userId = SecurityUtil.requireMpUserId();
    return ApiResult.ok(plazaService.listMyPostsSlice(userId, page, size));
  }

  @GetMapping("/public/plaza/users/{userId}/posts")
  public ApiResult<SliceResponse<PlazaPostItem>> userPosts(
      @PathVariable long userId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    userService.requireUser(userId);
    Long viewerId = optionalViewerId();
    return ApiResult.ok(plazaService.listUserPostsSlice(viewerId, userId, page, size));
  }

  @GetMapping("/public/plaza/users/{userId}/tasks")
  public ApiResult<SliceResponse<TaskListItem>> userTasks(
      @PathVariable long userId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    userService.requireUser(userId);
    return ApiResult.ok(taskService.listUserPublishedTasksSlice(userId, page, size));
  }

  @GetMapping("/public/plaza/users/{userId}/music")
  public ApiResult<SliceResponse<AiMusicService.MusicItem>> userMusic(
      @PathVariable long userId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    userService.requireUser(userId);
    Long viewerId = optionalViewerId();
    if (viewerId == null) return ApiResult.ok(musicService.publicUserMusicSlice(userId, page, size));
    return ApiResult.ok(musicService.publicUserMusicSlice(userService.requireUser(viewerId), userId, page, size));
  }

  public record MetaCategory(String code, String name) {}
  public record MetaAi(String code, String name, String abbr, String logoText, String logoUrl) {}
  public record MetaSort(String code, String name) {}
  public record PlazaMeta(List<MetaSort> sorts, List<MetaCategory> categories, List<MetaAi> aiProviders) {}

  @GetMapping("/public/plaza/meta")
  public ApiResult<PlazaMeta> meta() {
    String cacheKey = "cache:mp:plaza:meta:v1";
    PlazaMeta cached = redisSupport.getJson(cacheKey, PlazaMeta.class);
    if (cached != null) return ApiResult.ok(cached);
    var sorts = plazaService.listActiveSortOptions().stream()
        .map(s -> new MetaSort(s.getCode(), s.getName()))
        .toList();
    var categories = plazaService.listActiveCategories().stream()
        .map(c -> new MetaCategory(c.getCode(), c.getName()))
        .toList();
    var providers = plazaService.listActiveAiProviders().stream()
        .map(a -> new MetaAi(a.getCode(), a.getName(), a.getAbbr(), a.getLogoText(), a.getLogoUrl()))
        .toList();
    PlazaMeta resp = new PlazaMeta(sorts, categories, providers);
    redisSupport.setJson(cacheKey, resp, Duration.ofSeconds(60));
    return ApiResult.ok(resp);
  }

  public record CreateResp(long postId) {}
  public record LikeResp(boolean liked) {}
  public record CommentReq(@NotBlank(message = "content is required") String content) {}
  public record CommentResp(long commentId) {}
  public record CommentItem(long id, long userId, String userName, String userAvatar, String content, String createdAt) {}
  public record FollowSummaryResp(long followingCount, long followerCount) {}
  public record FollowUserResp(
      long userId,
      String nickname,
      String avatar,
      String signature,
      long postCount,
      boolean followed
  ) {}
  public record UserCardResp(
      long userId,
      String nickname,
      String avatar,
      String signature,
      long postCount,
      long followingCount,
      long followerCount,
      boolean followed,
      boolean self
  ) {}

  @PostMapping("/plaza/posts")
  public ApiResult<CreateResp> create(@Valid @RequestBody PlazaCreateRequest req) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    long postId = plazaService.createPost(user, req);
    return ApiResult.ok(new CreateResp(postId));
  }

  @PostMapping("/plaza/posts/{postId}/like")
  public ApiResult<LikeResp> like(@PathVariable long postId) {
    long userId = SecurityUtil.requireMpUserId();
    var me = userService.requireUser(userId);
    boolean liked = plazaService.setLike(me, postId, true);
    return ApiResult.ok(new LikeResp(liked));
  }

  @DeleteMapping("/plaza/posts/{postId}/like")
  public ApiResult<LikeResp> unlike(@PathVariable long postId) {
    long userId = SecurityUtil.requireMpUserId();
    var me = userService.requireUser(userId);
    boolean liked = plazaService.setLike(me, postId, false);
    return ApiResult.ok(new LikeResp(liked));
  }

  @GetMapping("/public/plaza/posts/{postId}/comments")
  public ApiResult<List<CommentItem>> comments(@PathVariable long postId) {
    var list = plazaService.comments(postId).stream()
        .map(c -> new CommentItem(c.id(), c.userId(), c.userName(), c.userAvatar(), c.content(), c.createdAt().toString()))
        .toList();
    return ApiResult.ok(list);
  }

  @PostMapping("/plaza/posts/{postId}/comments")
  public ApiResult<CommentResp> comment(@PathVariable long postId, @Valid @RequestBody CommentReq req) {
    long userId = SecurityUtil.requireMpUserId();
    var me = userService.requireUser(userId);
    long id = plazaService.addComment(me, postId, req.content());
    return ApiResult.ok(new CommentResp(id));
  }

  @PostMapping("/plaza/follow/{targetUserId}")
  public ApiResult<Void> follow(@PathVariable long targetUserId) {
    long userId = SecurityUtil.requireMpUserId();
    var me = userService.requireUser(userId);
    userService.requireUser(targetUserId);
    plazaService.follow(me, targetUserId);
    return ApiResult.ok(null);
  }

  @DeleteMapping("/plaza/follow/{targetUserId}")
  public ApiResult<Void> unfollow(@PathVariable long targetUserId) {
    long userId = SecurityUtil.requireMpUserId();
    var me = userService.requireUser(userId);
    plazaService.unfollow(me, targetUserId);
    return ApiResult.ok(null);
  }

  @GetMapping("/plaza/follows/summary")
  public ApiResult<FollowSummaryResp> followSummary() {
    long userId = SecurityUtil.requireMpUserId();
    var s = plazaService.followSummary(userId);
    return ApiResult.ok(new FollowSummaryResp(s.followingCount(), s.followerCount()));
  }

  @GetMapping("/plaza/follows/following")
  public ApiResult<SliceResponse<FollowUserResp>> following(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    long userId = SecurityUtil.requireMpUserId();
    var slice = plazaService.listFollowingSlice(userId, page, size);
    var list = slice.items().stream()
        .map(v -> new FollowUserResp(v.userId(), v.nickname(), v.avatar(), v.signature(), v.postCount(), v.followed()))
        .toList();
    return ApiResult.ok(new SliceResponse<>(list, slice.page(), slice.size(), slice.hasMore()));
  }

  @GetMapping("/plaza/follows/followers")
  public ApiResult<SliceResponse<FollowUserResp>> followers(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    long userId = SecurityUtil.requireMpUserId();
    var slice = plazaService.listFollowersSlice(userId, page, size);
    var list = slice.items().stream()
        .map(v -> new FollowUserResp(v.userId(), v.nickname(), v.avatar(), v.signature(), v.postCount(), v.followed()))
        .toList();
    return ApiResult.ok(new SliceResponse<>(list, slice.page(), slice.size(), slice.hasMore()));
  }

  @GetMapping("/public/plaza/users/{userId}/card")
  public ApiResult<UserCardResp> userCard(@PathVariable long userId) {
    var target = userService.requireUser(userId);
    Long viewerId = optionalViewerId();
    var summary = plazaService.followSummary(userId);
    long postCount = plazaService.postCount(userId);
    boolean followed = viewerId != null && plazaService.isFollowing(viewerId, userId);
    boolean self = viewerId != null && viewerId == userId;
    String nickname = resolveUserName(target.getNickname(), target.getOpenId());
    String avatar = target.getAvatar() == null ? "" : target.getAvatar().trim();
    String signature = target.getSignature() == null ? "" : target.getSignature().trim();
    return ApiResult.ok(new UserCardResp(
        userId,
        nickname,
        avatar,
        signature,
        postCount,
        summary.followingCount(),
        summary.followerCount(),
        followed,
        self
    ));
  }

  private Long optionalViewerId() {
    try {
      return SecurityUtil.requireMpUserId();
    } catch (Exception e) {
      return null;
    }
  }

  private static String resolveUserName(String nickname, String openId) {
    String n = nickname == null ? "" : nickname.trim();
    if (!n.isEmpty()) return n;
    String open = openId == null ? "" : openId.trim();
    if (open.length() > 4) return "用户" + open.substring(open.length() - 4);
    return "用户";
  }
}
