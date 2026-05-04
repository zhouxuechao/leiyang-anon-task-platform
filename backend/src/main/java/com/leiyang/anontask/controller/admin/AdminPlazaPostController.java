package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.PlazaPost;
import com.leiyang.anontask.domain.PlazaPostComment;
import com.leiyang.anontask.repo.AiCommentJobRepository;
import com.leiyang.anontask.repo.AiMusicJobRepository;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.PlazaCategoryRepository;
import com.leiyang.anontask.repo.PlazaPostCommentRepository;
import com.leiyang.anontask.repo.PlazaPostImageRepository;
import com.leiyang.anontask.repo.PlazaPostLikeRepository;
import com.leiyang.anontask.repo.PlazaPostRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/plaza/posts")
public class AdminPlazaPostController {
  private final PlazaPostRepository postRepo;
  private final PlazaPostImageRepository imageRepo;
  private final PlazaPostCommentRepository commentRepo;
  private final PlazaPostLikeRepository likeRepo;
  private final PlazaCategoryRepository categoryRepo;
  private final AiCommentJobRepository aiCommentJobRepo;
  private final AiMusicJobRepository musicJobRepo;

  public AdminPlazaPostController(
      PlazaPostRepository postRepo,
      PlazaPostImageRepository imageRepo,
      PlazaPostCommentRepository commentRepo,
      PlazaPostLikeRepository likeRepo,
      PlazaCategoryRepository categoryRepo,
      AiCommentJobRepository aiCommentJobRepo,
      AiMusicJobRepository musicJobRepo
  ) {
    this.postRepo = postRepo;
    this.imageRepo = imageRepo;
    this.commentRepo = commentRepo;
    this.likeRepo = likeRepo;
    this.categoryRepo = categoryRepo;
    this.aiCommentJobRepo = aiCommentJobRepo;
    this.musicJobRepo = musicJobRepo;
  }

  public record PostItem(
      long id,
      long authorId,
      String authorName,
      String category,
      String categoryName,
      String content,
      int likeCount,
      int commentCount,
      boolean hot,
      Instant createdAt
  ) {}

  public record PostCommentItem(
      long id,
      long userId,
      String userName,
      String userAvatar,
      String content,
      Instant createdAt
  ) {}

  public record PostDetail(
      long id,
      long authorId,
      String authorName,
      String authorAvatar,
      String category,
      String categoryName,
      String content,
      List<String> images,
      int likeCount,
      int commentCount,
      boolean hot,
      Instant createdAt,
      List<PostCommentItem> comments
  ) {}

  public record HotReq(boolean hot) {}

  @GetMapping("")
  public ApiResult<PageResponse<PostItem>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String category
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(100, Math.max(1, size));
    String query = trim(q);
    String cat = normalizeCategory(category);
    var pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<PlazaPost> p = postRepo.adminSearch(cat, query, pageable);

    Map<String, String> categoryNameMap = categoryRepo.findAll().stream()
        .collect(Collectors.toMap(v -> normalizeCategory(v.getCode()), v -> trim(v.getName()), (a, b) -> a));

    var items = p.getContent().stream().map(v -> new PostItem(
        v.getId(),
        v.getAuthor().getId(),
        displayName(v),
        v.getCategory(),
        categoryNameMap.getOrDefault(normalizeCategory(v.getCategory()), normalizeCategory(v.getCategory())),
        v.getContent(),
        v.getLikeCount(),
        (int) commentRepo.countByPost(v),
        v.isHotFlag(),
        v.getCreatedAt()
    )).toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), items));
  }

  @GetMapping("/{id}")
  public ApiResult<PostDetail> detail(@PathVariable Long id) {
    PlazaPost post = postRepo.findById(id).orElseThrow(() -> new BizException("Post not found"));
    String cat = normalizeCategory(post.getCategory());
    String catName = categoryRepo.findByCode(cat).map(v -> trim(v.getName())).orElse(cat);
    var images = imageRepo.findByPostOrderBySortNoAsc(post).stream().map(v -> v.getImageUrl()).toList();
    var comments = commentRepo.findByPostOrderByCreatedAtAsc(post).stream().map(v -> new PostCommentItem(
        v.getId(),
        v.getUser().getId(),
        displayName(v.getUser().getNickname(), v.getUser().getOpenId()),
        trim(v.getUser().getAvatar()),
        v.getContent(),
        v.getCreatedAt()
    )).toList();
    var detail = new PostDetail(
        post.getId(),
        post.getAuthor().getId(),
        displayName(post),
        trim(post.getAuthor().getAvatar()),
        post.getCategory(),
        catName,
        post.getContent(),
        images,
        post.getLikeCount(),
        (int) commentRepo.countByPost(post),
        post.isHotFlag(),
        post.getCreatedAt(),
        comments
    );
    return ApiResult.ok(detail);
  }

  @PostMapping("/{id}/hot")
  @Transactional
  public ApiResult<Void> setHot(@PathVariable Long id, @RequestBody HotReq req) {
    PlazaPost post = postRepo.findById(id).orElseThrow(() -> new BizException("Post not found"));
    post.setHotFlag(req != null && req.hot());
    postRepo.save(post);
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/delete")
  @Transactional
  public ApiResult<Void> delete(@PathVariable Long id) {
    PlazaPost post = postRepo.findById(id).orElseThrow(() -> new BizException("Post not found"));
    musicJobRepo.findAllByPlazaPostId(post.getId()).forEach(job -> {
      job.setPlazaPostId(null);
      musicJobRepo.save(job);
    });
    aiCommentJobRepo.deleteByPost(post);
    likeRepo.deleteByPost(post);
    commentRepo.deleteByPost(post);
    imageRepo.deleteByPost(post);
    postRepo.delete(post);
    return ApiResult.ok(null);
  }

  @PostMapping("/comments/{commentId}/delete")
  @Transactional
  public ApiResult<Void> deleteComment(@PathVariable Long commentId) {
    PlazaPostComment c = commentRepo.findById(commentId).orElseThrow(() -> new BizException("Comment not found"));
    PlazaPost post = c.getPost();
    commentRepo.delete(c);
    post.setCommentCount((int) commentRepo.countByPost(post));
    postRepo.save(post);
    return ApiResult.ok(null);
  }

  private static String displayName(PlazaPost post) {
    return displayName(post.getAuthor().getNickname(), post.getAuthor().getOpenId());
  }

  private static String displayName(String nickname, String openId) {
    String n = trim(nickname);
    if (!n.isEmpty()) return n;
    String open = trim(openId);
    if (open.length() > 4) return "用户" + open.substring(open.length() - 4);
    return "用户";
  }

  private static String normalizeCategory(String s) {
    String v = trim(s).toUpperCase(Locale.ROOT).replace('-', '_');
    if ("ALL".equals(v)) return "";
    return v;
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
