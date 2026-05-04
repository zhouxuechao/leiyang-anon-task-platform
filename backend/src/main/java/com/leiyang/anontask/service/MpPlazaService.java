package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.PlazaAiProvider;
import com.leiyang.anontask.domain.PlazaCategory;
import com.leiyang.anontask.domain.PlazaFollow;
import com.leiyang.anontask.domain.PlazaPost;
import com.leiyang.anontask.domain.PlazaPostComment;
import com.leiyang.anontask.domain.PlazaPostImage;
import com.leiyang.anontask.domain.PlazaPostLike;
import com.leiyang.anontask.domain.PlazaSortOption;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.AiMusicJob;
import com.leiyang.anontask.domain.enums.GeneralStatus;
import com.leiyang.anontask.domain.enums.SensitiveWordAction;
import com.leiyang.anontask.dto.mp.PlazaCreateRequest;
import com.leiyang.anontask.dto.mp.PlazaPostItem;
import com.leiyang.anontask.dto.mp.SliceResponse;
import com.leiyang.anontask.repo.PlazaAiProviderRepository;
import com.leiyang.anontask.repo.PlazaCategoryRepository;
import com.leiyang.anontask.repo.PlazaFollowRepository;
import com.leiyang.anontask.repo.PlazaPostImageRepository;
import com.leiyang.anontask.repo.PlazaPostCommentRepository;
import com.leiyang.anontask.repo.PlazaPostLikeRepository;
import com.leiyang.anontask.repo.PlazaPostRepository;
import com.leiyang.anontask.repo.PlazaSortOptionRepository;
import com.leiyang.anontask.repo.AiMusicJobRepository;
import com.leiyang.anontask.util.Slices;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MpPlazaService {
  private static final Logger log = LoggerFactory.getLogger(MpPlazaService.class);
  private static final String CATEGORY_OTHER = "OTHER";
  private static final String DELETED_MUSIC_HINT = "作者已删除歌曲";
  private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,6}|[A-Za-z0-9]{3,12}");
  private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
      "这个", "那个", "今天", "明天", "我们", "你们", "他们", "以及", "然后", "非常", "真的", "就是", "可以", "一下",
      "daily", "share", "record", "hello", "thanks"
  ));

  private final PlazaPostRepository postRepo;
  private final PlazaPostImageRepository imageRepo;
  private final PlazaPostLikeRepository likeRepo;
  private final PlazaPostCommentRepository commentRepo;
  private final PlazaFollowRepository followRepo;
  private final PlazaCategoryRepository categoryRepo;
  private final PlazaAiProviderRepository aiProviderRepo;
  private final PlazaSortOptionRepository sortOptionRepo;
  private final SensitiveWordService sensitiveWordService;
  private final AiCategoryClassifierService aiClassifierService;
  private final AiAutomationService aiAutomationService;
  private final AiMusicJobRepository musicRepo;
  private final RedisSupportService redisSupport;

  public MpPlazaService(
      PlazaPostRepository postRepo,
      PlazaPostImageRepository imageRepo,
      PlazaPostLikeRepository likeRepo,
      PlazaPostCommentRepository commentRepo,
      PlazaFollowRepository followRepo,
      PlazaCategoryRepository categoryRepo,
      PlazaAiProviderRepository aiProviderRepo,
      PlazaSortOptionRepository sortOptionRepo,
      SensitiveWordService sensitiveWordService,
      AiCategoryClassifierService aiClassifierService,
      AiAutomationService aiAutomationService,
      AiMusicJobRepository musicRepo,
      RedisSupportService redisSupport
  ) {
    this.postRepo = postRepo;
    this.imageRepo = imageRepo;
    this.likeRepo = likeRepo;
    this.commentRepo = commentRepo;
    this.followRepo = followRepo;
    this.categoryRepo = categoryRepo;
    this.aiProviderRepo = aiProviderRepo;
    this.sortOptionRepo = sortOptionRepo;
    this.sensitiveWordService = sensitiveWordService;
    this.aiClassifierService = aiClassifierService;
    this.aiAutomationService = aiAutomationService;
    this.musicRepo = musicRepo;
    this.redisSupport = redisSupport;
  }

  @Transactional
  public long createPost(UserAccount author, PlazaCreateRequest req) {
    if (!redisSupport.setIfAbsent("lock:plaza:post:user:" + author.getId(), String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(10))) {
      throw new BizException("动态正在发布，请勿重复点击");
    }
    String content = trim(req.content());
    if (content.length() > 500) throw new BizException("content too long");
    List<String> images = req.images() == null ? List.of() : req.images();
    if (images.size() > 9) throw new BizException("max 9 images");
    if (content.isEmpty() && images.isEmpty()) throw new BizException("content or images is required");
    if (sensitiveWordService.evaluate(content) == SensitiveWordAction.REJECT) {
      throw new BizException("Post contains sensitive content");
    }

    String category = resolveCategoryCode(content, req.category());
    String gender = normalizeGender(req.gender(), category);
    PlazaPost post = new PlazaPost();
    post.setAuthor(author);
    post.setContent(content);
    post.setGender(gender);
    post.setCategory(category);
    post.setLikeCount(0);
    post.setCommentCount(0);
    post.setHotFlag(false);
    postRepo.save(post);

    int i = 0;
    for (String url : images) {
      String u = trim(url);
      if (u.isEmpty()) continue;
      PlazaPostImage img = new PlazaPostImage();
      img.setPost(post);
      img.setImageUrl(u);
      img.setSortNo(i++);
      img.setCreatedAt(Instant.now());
      imageRepo.save(img);
    }
    aiAutomationService.autoCommentForNewPost(post);
    log.info("plaza_post_created postId={} authorId={} category={} gender={} imageCount={}", post.getId(), author.getId(), category, gender, i);
    return post.getId();
  }

  @Transactional
  public void follow(UserAccount me, long targetUserId) {
    if (me.getId() == targetUserId) throw new BizException("cannot follow yourself");
    UserAccount target = new UserAccount();
    target.setId(targetUserId);
    if (followRepo.findByUserAndTargetUser(me, target).isPresent()) return;
    PlazaFollow f = new PlazaFollow();
    f.setUser(me);
    f.setTargetUser(target);
    f.setCreatedAt(Instant.now());
    followRepo.save(f);
    log.info("plaza_follow userId={} targetUserId={}", me.getId(), targetUserId);
  }

  @Transactional
  public void unfollow(UserAccount me, long targetUserId) {
    UserAccount target = new UserAccount();
    target.setId(targetUserId);
    followRepo.findByUserAndTargetUser(me, target).ifPresent(f -> {
      followRepo.delete(f);
      log.info("plaza_unfollow userId={} targetUserId={}", me.getId(), targetUserId);
    });
  }

  @Transactional
  public boolean setLike(UserAccount me, long postId, boolean wantLike) {
    PlazaPost post = postRepo.findById(postId).orElseThrow(() -> new BizException("Post not found"));
    var existed = likeRepo.findByPostAndUser(post, me);
    if (existed.isPresent() && !wantLike) {
      likeRepo.delete(existed.get());
      post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
      postRepo.save(post);
      log.info("plaza_post_unliked postId={} userId={} likeCount={}", postId, me.getId(), post.getLikeCount());
      return false;
    }
    if (existed.isPresent()) return true;
    PlazaPostLike like = new PlazaPostLike();
    like.setPost(post);
    like.setUser(me);
    likeRepo.save(like);
    post.setLikeCount(post.getLikeCount() + 1);
    postRepo.save(post);
    log.info("plaza_post_liked postId={} userId={} likeCount={}", postId, me.getId(), post.getLikeCount());
    return true;
  }

  @Transactional
  public long addComment(UserAccount me, long postId, String content) {
    if (!redisSupport.setIfAbsent("lock:plaza:comment:" + postId + ":user:" + me.getId(), String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(5))) {
      throw new BizException("评论正在提交，请勿重复点击");
    }
    PlazaPost post = postRepo.findById(postId).orElseThrow(() -> new BizException("Post not found"));
    String txt = trim(content);
    if (txt.isEmpty()) throw new BizException("comment is required");
    if (txt.length() > 300) throw new BizException("comment too long");
    if (sensitiveWordService.evaluate(txt) == SensitiveWordAction.REJECT) {
      throw new BizException("Comment contains sensitive content");
    }
    PlazaPostComment c = new PlazaPostComment();
    c.setPost(post);
    c.setUser(me);
    c.setContent(txt);
    commentRepo.save(c);
    post.setCommentCount((int) commentRepo.countByPost(post));
    postRepo.save(post);
    log.info("plaza_comment_added postId={} commentId={} userId={} commentCount={}", postId, c.getId(), me.getId(), post.getCommentCount());
    return c.getId();
  }

  @Transactional
  public void markMusicDeleted(long postId) {
    if (postId <= 0) return;
    postRepo.findById(postId).ifPresent(post -> {
      String content = deletedMusicContent(post);
      if (!content.equals(trim(post.getContent()))) {
        post.setContent(cutLen(content, 500));
        postRepo.save(post);
        log.info("plaza_music_post_mark_deleted postId={}", postId);
      }
    });
  }

  public List<CommentItem> comments(long postId) {
    PlazaPost post = postRepo.findById(postId).orElseThrow(() -> new BizException("Post not found"));
    return commentRepo.findByPostOrderByCreatedAtAsc(post).stream()
        .map(c -> new CommentItem(
            c.getId(),
            c.getUser().getId(),
            displayName(c.getUser()),
            trim(c.getUser().getAvatar()),
            c.getContent(),
            c.getCreatedAt()
        ))
        .toList();
  }

  public List<PlazaCategory> listActiveCategories() {
    return categoryRepo.findByStatusOrderBySortNoAscIdAsc(GeneralStatus.ACTIVE.name());
  }

  public List<PlazaAiProvider> listActiveAiProviders() {
    return aiProviderRepo.findByStatusOrderBySortNoAscIdAsc(GeneralStatus.ACTIVE.name());
  }

  public List<PlazaSortOption> listActiveSortOptions() {
    return sortOptionRepo.findByStatusOrderBySortNoAscIdAsc(GeneralStatus.ACTIVE.name());
  }

  @Transactional
  public List<PlazaPostItem> list(Long viewerId, String sort, String gender, String category, int page, int size) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 30);
    String g = normalizeGenderOrAll(gender);
    String c = normalizeCategoryOrAll(category);
    String s = normalizeSort(sort);
    Sort dbSort = "HOT".equals(s)
        ? Sort.by(
            Sort.Order.desc("hotFlag"),
            Sort.Order.desc("likeCount"),
            Sort.Order.desc("commentCount"),
            Sort.Order.desc("createdAt")
        )
        : Sort.by(Sort.Order.desc("createdAt"));
    var posts = postRepo.publicSearchItems(g, c, "FOLLOW".equals(s), viewerId, PageRequest.of(safePage - 1, safeSize, dbSort));
    Map<Long, List<String>> imageMap = imageMap(posts);
    Map<Long, PlazaPostItem.MusicAttachment> musicMap = musicAttachmentMap(posts);
    Map<String, String> categoryNameMap = categoryNameMap(posts);

    Set<Long> followedSet = new HashSet<>();
    Set<Long> likedSet = new HashSet<>();
    if (viewerId != null) {
      UserAccount me = new UserAccount();
      me.setId(viewerId);
      followedSet = followRepo.findByUser_Id(viewerId).stream().map(f -> f.getTargetUser().getId()).collect(java.util.stream.Collectors.toSet());
      likedSet = likeRepo.findByUserAndPostIn(me, posts).stream().map(v -> v.getPost().getId()).collect(java.util.stream.Collectors.toSet());
    }
    final Set<Long> finalFollowedSet = followedSet;
    final Set<Long> finalLikedSet = likedSet;
    final Long viewer = viewerId;

    return posts.stream()
        .map(p -> new PlazaPostItem(
            p.getId(),
            p.getAuthor().getId(),
            displayName(p.getAuthor()),
            trim(p.getAuthor().getAvatar()),
            normalizeGenderForQuery(p.getGender()),
            normalizeCategoryCode(p.getCategory()),
            categoryNameMap.getOrDefault(normalizeCategoryCode(p.getCategory()), normalizeCategoryCode(p.getCategory())),
            displayContent(p, musicMap),
            imageMap.getOrDefault(p.getId(), List.of()),
            p.getLikeCount(),
            p.getCommentCount(),
            viewer != null && finalLikedSet.contains(p.getId()),
            viewer != null && finalFollowedSet.contains(p.getAuthor().getId()),
            p.isHotFlag(),
            p.getCreatedAt(),
            musicAttachmentForPost(p, musicMap)
        ))
        .toList();
  }

  @Transactional
  public SliceResponse<PlazaPostItem> listSlice(Long viewerId, String sort, String gender, String category, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 30);
    List<PlazaPostItem> rows = list(viewerId, sort, gender, category, safePage, safeSize + 1);
    return Slices.of(rows, safePage, safeSize);
  }

  @Transactional
  public List<PlazaPostItem> listMyPosts(long userId, int page, int size) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 30);
    var posts = postRepo.findAuthorItems(userId, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createdAt"))));
    Map<Long, List<String>> imageMap = imageMap(posts);
    Map<Long, PlazaPostItem.MusicAttachment> musicMap = musicAttachmentMap(posts);
    Map<String, String> categoryNameMap = categoryNameMap(posts);
    UserAccount me = new UserAccount();
    me.setId(userId);
    var likedSet = likeRepo.findByUserAndPostIn(me, posts).stream().map(v -> v.getPost().getId()).collect(java.util.stream.Collectors.toSet());

    return posts.stream()
        .map(p -> new PlazaPostItem(
            p.getId(),
            p.getAuthor().getId(),
            displayName(p.getAuthor()),
            trim(p.getAuthor().getAvatar()),
            normalizeGenderForQuery(p.getGender()),
            normalizeCategoryCode(p.getCategory()),
            categoryNameMap.getOrDefault(normalizeCategoryCode(p.getCategory()), normalizeCategoryCode(p.getCategory())),
            displayContent(p, musicMap),
            imageMap.getOrDefault(p.getId(), List.of()),
            p.getLikeCount(),
            p.getCommentCount(),
            likedSet.contains(p.getId()),
            false,
            p.isHotFlag(),
            p.getCreatedAt(),
            musicAttachmentForPost(p, musicMap)
        ))
        .toList();
  }

  @Transactional
  public SliceResponse<PlazaPostItem> listMyPostsSlice(long userId, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 30);
    List<PlazaPostItem> rows = listMyPosts(userId, safePage, safeSize + 1);
    return Slices.of(rows, safePage, safeSize);
  }

  @Transactional
  public List<PlazaPostItem> listUserPosts(Long viewerId, long authorId, int page, int size) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 30);
    var posts = postRepo.findAuthorItems(authorId, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createdAt"))));
    Map<Long, List<String>> imageMap = imageMap(posts);
    Map<Long, PlazaPostItem.MusicAttachment> musicMap = musicAttachmentMap(posts);
    Map<String, String> categoryNameMap = categoryNameMap(posts);
    Set<Long> likedSet = new HashSet<>();
    Set<Long> followedSet = new HashSet<>();
    if (viewerId != null) {
      UserAccount me = new UserAccount();
      me.setId(viewerId);
      likedSet = likeRepo.findByUserAndPostIn(me, posts).stream().map(v -> v.getPost().getId()).collect(java.util.stream.Collectors.toSet());
      followedSet = followRepo.findByUser_Id(viewerId).stream().map(f -> f.getTargetUser().getId()).collect(java.util.stream.Collectors.toSet());
    }
    final Set<Long> finalLikedSet = likedSet;
    final Set<Long> finalFollowedSet = followedSet;
    return posts.stream()
        .map(p -> new PlazaPostItem(
            p.getId(),
            p.getAuthor().getId(),
            displayName(p.getAuthor()),
            trim(p.getAuthor().getAvatar()),
            normalizeGenderForQuery(p.getGender()),
            normalizeCategoryCode(p.getCategory()),
            categoryNameMap.getOrDefault(normalizeCategoryCode(p.getCategory()), normalizeCategoryCode(p.getCategory())),
            displayContent(p, musicMap),
            imageMap.getOrDefault(p.getId(), List.of()),
            p.getLikeCount(),
            p.getCommentCount(),
            viewerId != null && finalLikedSet.contains(p.getId()),
            viewerId != null && finalFollowedSet.contains(p.getAuthor().getId()),
            p.isHotFlag(),
            p.getCreatedAt(),
            musicAttachmentForPost(p, musicMap)
        ))
        .toList();
  }

  @Transactional
  public SliceResponse<PlazaPostItem> listUserPostsSlice(Long viewerId, long authorId, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 30);
    List<PlazaPostItem> rows = listUserPosts(viewerId, authorId, safePage, safeSize + 1);
    return Slices.of(rows, safePage, safeSize);
  }

  private Map<Long, List<String>> imageMap(List<PlazaPost> posts) {
    if (posts == null || posts.isEmpty()) return Collections.emptyMap();
    return imageRepo.findByPostInOrderByPostIdAscSortNoAsc(posts).stream()
        .collect(java.util.stream.Collectors.groupingBy(
            img -> img.getPost().getId(),
            java.util.stream.Collectors.mapping(PlazaPostImage::getImageUrl, java.util.stream.Collectors.toList())
        ));
  }

  private Map<Long, PlazaPostItem.MusicAttachment> musicAttachmentMap(List<PlazaPost> posts) {
    if (posts == null || posts.isEmpty()) return Collections.emptyMap();
    var ids = posts.stream().map(PlazaPost::getId).toList();
    return musicRepo.findByPlazaPostIdIn(ids).stream()
        .filter(job -> job.getPlazaPostId() != null)
        .collect(java.util.stream.Collectors.toMap(
            AiMusicJob::getPlazaPostId,
            this::toMusicAttachment,
            (a, b) -> a
        ));
  }

  private PlazaPostItem.MusicAttachment musicAttachmentForPost(PlazaPost post, Map<Long, PlazaPostItem.MusicAttachment> musicMap) {
    PlazaPostItem.MusicAttachment attachment = musicMap.get(post.getId());
    if (attachment != null) return attachment;
    if (isMusicPost(post)) return deletedMusicAttachment();
    return null;
  }

  private String displayContent(PlazaPost post, Map<Long, PlazaPostItem.MusicAttachment> musicMap) {
    if (!isMusicPost(post) || musicMap.containsKey(post.getId())) return trim(post.getContent());
    return deletedMusicContent(post);
  }

  private static boolean isMusicPost(PlazaPost post) {
    if (post == null) return false;
    if ("MUSIC".equals(normalizeCategoryCode(post.getCategory()))) return true;
    String content = trim(post.getContent());
    return content.startsWith("我生成了一首AI歌曲《") || content.contains("AI歌曲《");
  }

  private static String deletedMusicContent(PlazaPost post) {
    String content = trim(post == null ? "" : post.getContent());
    if (content.contains(DELETED_MUSIC_HINT)) return content;
    return content.isEmpty() ? DELETED_MUSIC_HINT : content + "（" + DELETED_MUSIC_HINT + "）";
  }

  private PlazaPostItem.MusicAttachment deletedMusicAttachment() {
    return new PlazaPostItem.MusicAttachment(
        0,
        DELETED_MUSIC_HINT,
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        true
    );
  }

  private Map<String, String> categoryNameMap(List<PlazaPost> posts) {
    if (posts == null || posts.isEmpty()) return Collections.emptyMap();
    Set<String> codes = posts.stream()
        .map(p -> normalizeCategoryCode(p.getCategory()))
        .filter(v -> !v.isEmpty())
        .collect(java.util.stream.Collectors.toSet());
    if (codes.isEmpty()) return Collections.emptyMap();
    return categoryRepo.findAll().stream()
        .filter(v -> codes.contains(normalizeCategoryCode(v.getCode())))
        .collect(java.util.stream.Collectors.toMap(
            v -> normalizeCategoryCode(v.getCode()),
            v -> trim(v.getName()),
            (a, b) -> a
        ));
  }

  private PlazaPostItem.MusicAttachment musicAttachment(PlazaPost post) {
    if (post == null) return null;
    return musicRepo.findByPlazaPostId(post.getId())
        .map(this::toMusicAttachment)
        .orElse(null);
  }

  private PlazaPostItem.MusicAttachment toMusicAttachment(AiMusicJob job) {
    return new PlazaPostItem.MusicAttachment(
        job.getId(),
        job.getTitle(),
        trim(job.getAudioUrl()),
        trim(job.getImageUrl()),
        trim(job.getVideoUrl()),
        trim(job.getDuration()),
        displayName(job.getUser()),
        job.isInstrumental() ? "纯音乐" : (job.isCustomMode() ? displayName(job.getUser()) : "AI作词"),
        "AI音乐生成",
        job.isInstrumental() ? "" : trim(job.getPrompt()),
        trim(job.getStyle()),
        false
    );
  }

  public FollowSummary followSummary(long userId) {
    UserAccount me = new UserAccount();
    me.setId(userId);
    long following = followRepo.countByUser(me);
    long followers = followRepo.countByTargetUser(me);
    return new FollowSummary(following, followers);
  }

  public List<FollowUserItem> listFollowing(long userId, int page, int size) {
    return listFollowsInternal(userId, page, size, true);
  }

  @Transactional
  public SliceResponse<FollowUserItem> listFollowingSlice(long userId, int page, int size) {
    return listFollowsSliceInternal(userId, page, size, true);
  }

  public List<FollowUserItem> listFollowers(long userId, int page, int size) {
    return listFollowsInternal(userId, page, size, false);
  }

  @Transactional
  public SliceResponse<FollowUserItem> listFollowersSlice(long userId, int page, int size) {
    return listFollowsSliceInternal(userId, page, size, false);
  }

  public long postCount(long userId) {
    return postRepo.countByAuthorId(userId);
  }

  public boolean isFollowing(long viewerId, long targetUserId) {
    if (viewerId <= 0 || targetUserId <= 0 || viewerId == targetUserId) return false;
    UserAccount me = new UserAccount();
    me.setId(viewerId);
    UserAccount target = new UserAccount();
    target.setId(targetUserId);
    return followRepo.findByUserAndTargetUser(me, target).isPresent();
  }

  public record CommentItem(long id, long userId, String userName, String userAvatar, String content, Instant createdAt) {}
  public record FollowSummary(long followingCount, long followerCount) {}
  public record FollowUserItem(
      long userId,
      String nickname,
      String avatar,
      String signature,
      long postCount,
      boolean followed
  ) {}

  private List<FollowUserItem> listFollowsInternal(long userId, int page, int size, boolean followingList) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 30);
    UserAccount me = new UserAccount();
    me.setId(userId);
    var followsPage = followingList
        ? followRepo.findFollowingItems(me, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createdAt"))))
        : followRepo.findFollowerItems(me, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createdAt"))));
    var userIds = followsPage.stream()
        .map(f -> followingList ? f.getTargetUser().getId() : f.getUser().getId())
        .toList();
    Map<Long, Long> postCounts = userIds.isEmpty()
        ? Collections.emptyMap()
        : postRepo.countByAuthorIds(userIds).stream().collect(java.util.stream.Collectors.toMap(
            PlazaPostRepository.AuthorPostCount::getAuthorId,
            PlazaPostRepository.AuthorPostCount::getPostCount
        ));
    Set<Long> myFollowing = followingList
        ? new HashSet<>(userIds)
        : userIds.isEmpty()
            ? Collections.emptySet()
            : followRepo.findByUser_IdAndTargetUser_IdIn(userId, userIds).stream()
            .map(f -> f.getTargetUser().getId())
            .collect(java.util.stream.Collectors.toSet());
    return followsPage.stream().map(f -> {
      UserAccount u = followingList ? f.getTargetUser() : f.getUser();
      String nickname = displayName(u);
      String avatar = trim(u.getAvatar());
      String signature = trim(u.getSignature());
      long postCount = postCounts.getOrDefault(u.getId(), 0L);
      boolean followed = myFollowing.contains(u.getId());
      return new FollowUserItem(u.getId(), nickname, avatar, signature, postCount, followed);
    }).toList();
  }

  private SliceResponse<FollowUserItem> listFollowsSliceInternal(long userId, int page, int size, boolean followingList) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 30);
    List<FollowUserItem> rows = listFollowsInternal(userId, safePage, safeSize + 1, followingList);
    return Slices.of(rows, safePage, safeSize);
  }

  private static String normalizeSort(String s) {
    String v = trim(s).toUpperCase(Locale.ROOT);
    if ("HOT".equals(v) || "FOLLOW".equals(v)) return v;
    return "LATEST";
  }

  private static String normalizeGenderOrAll(String s) {
    String v = trim(s).toUpperCase(Locale.ROOT);
    if ("MALE".equals(v) || "FEMALE".equals(v)) return v;
    return "ALL";
  }

  private static String normalizeGender(String s, String categoryCode) {
    String v = trim(s).toUpperCase(Locale.ROOT);
    if ("FEMALE".equals(v) || "FEMALE".equals(categoryCode)) return "FEMALE";
    if ("MALE".equals(v) || "MALE".equals(categoryCode)) return "MALE";
    return "UNKNOWN";
  }

  private static String normalizeGenderForQuery(String s) {
    String v = trim(s).toUpperCase(Locale.ROOT);
    if ("FEMALE".equals(v)) return "FEMALE";
    if ("MALE".equals(v)) return "MALE";
    return "UNKNOWN";
  }

  private static boolean matchesGenderFilter(String genderFilter, PlazaPost post) {
    if ("ALL".equals(genderFilter)) return true;
    String postGender = normalizeGenderForQuery(post.getGender());
    String postCategory = normalizeCategoryCode(post.getCategory());
    if ("MALE".equals(genderFilter)) {
      if ("MALE".equals(postCategory)) return true;
      if ("FEMALE".equals(postCategory)) return false;
      return "MALE".equals(postGender);
    }
    if ("FEMALE".equals(genderFilter)) {
      if ("FEMALE".equals(postCategory)) return true;
      if ("MALE".equals(postCategory)) return false;
      return "FEMALE".equals(postGender);
    }
    return genderFilter.equals(postGender);
  }

  private static String normalizeCategoryOrAll(String s) {
    String v = trim(s).toUpperCase(Locale.ROOT);
    if ("ALL".equals(v)) return "ALL";
    return normalizeCategoryCode(v);
  }

  private static String normalizeCategoryCode(String s) {
    String v = trim(s).toUpperCase(Locale.ROOT);
    if (v.isEmpty()) return "";
    return v.replace('-', '_');
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private static String cutLen(String s, int max) {
    String v = trim(s);
    return v.length() <= max ? v : v.substring(0, max);
  }

  private static String displayName(UserAccount u) {
    String n = trim(u.getNickname());
    if (!n.isEmpty()) return n;
    String open = trim(u.getOpenId());
    if (open.length() > 4) return "用户" + open.substring(open.length() - 4);
    return "用户";
  }

  private String resolveCategoryName(String categoryCode) {
    String code = normalizeCategoryCode(categoryCode);
    if (code.isEmpty()) return "其他";
    return categoryRepo.findByCode(code).map(PlazaCategory::getName).orElse(code);
  }

  @Transactional
  protected String resolveCategoryCode(String content, String requestedCategory) {
    String categoryCode = normalizeCategoryCode(requestedCategory);
    if ("MUSIC".equals(categoryCode)) return "MUSIC";
    // manual selection: honor selected category directly
    if (!categoryCode.isEmpty() && !"AUTO".equals(categoryCode) && categoryRepo.findByCode(categoryCode).isPresent()) {
      return categoryCode;
    }

    // AUTO mode: only match existing active categories
    List<PlazaCategory> active = listActiveCategories();
    var aiResult = aiClassifierService.classify(content, active).orElse(null);
    if (aiResult != null && !trim(aiResult.matchedCode()).isEmpty()) {
      return normalizeCategoryCode(aiResult.matchedCode());
    }
    if (aiResult != null && "OTHER".equals(normalizeCategoryCode(aiResult.matchedCode()))) {
      ensureOtherCategoryExists();
      createPendingCategoryIfAbsent(aiResult.suggestedName());
      return CATEGORY_OTHER;
    }

    PlazaCategory best = findBestMatch(content, active);
    if (best != null) return best.getCode();

    // not matched: fallback to OTHER and create inactive candidate for admin review
    ensureOtherCategoryExists();

    String newName = buildDynamicCategoryName(content);
    createPendingCategoryIfAbsent(newName);
    return CATEGORY_OTHER;
  }

  private PlazaCategory findBestMatch(String content, List<PlazaCategory> categories) {
    String text = trim(content).toLowerCase(Locale.ROOT);
    if (text.isEmpty()) return null;
    PlazaCategory best = null;
    int bestScore = 0;
    for (PlazaCategory c : categories) {
      int score = 0;
      for (String k : splitKeywords(c)) {
        if (!k.isEmpty() && text.contains(k.toLowerCase(Locale.ROOT))) score += Math.max(1, k.length());
      }
      if (score > bestScore) {
        bestScore = score;
        best = c;
      }
    }
    return best;
  }

  private static List<String> splitKeywords(PlazaCategory c) {
    String source = trim(c.getName()) + "," + trim(c.getKeywords());
    return Arrays.stream(source.split("[,，、\\s]+")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private static String buildDynamicCategoryName(String content) {
    String text = trim(content);
    Matcher m = TOKEN_PATTERN.matcher(text);
    while (m.find()) {
      String token = m.group().trim();
      if (token.length() < 2) continue;
      if (STOP_WORDS.contains(token.toLowerCase(Locale.ROOT))) continue;
      if (token.length() > 6) token = token.substring(0, 6);
      return token;
    }
    return "";
  }

  private static String buildCategoryCode(String name) {
    String seed = trim(name).toUpperCase(Locale.ROOT);
    if (seed.isEmpty()) seed = "AUTO";
    String hash = Integer.toHexString(seed.hashCode()).toUpperCase(Locale.ROOT);
    hash = hash.replace("-", "N");
    if (hash.length() > 8) hash = hash.substring(0, 8);
    return "AI_" + hash;
  }

  private void ensureOtherCategoryExists() {
    if (categoryRepo.findByCode(CATEGORY_OTHER).isPresent()) return;
    PlazaCategory c = new PlazaCategory();
    c.setCode(CATEGORY_OTHER);
    c.setName("其他");
    c.setKeywords("其他,未分类");
    c.setStatus(GeneralStatus.ACTIVE.name());
    c.setSortNo(999);
    categoryRepo.save(c);
  }

  private void createPendingCategoryIfAbsent(String newName) {
    String name = trim(newName);
    if (name.isEmpty()) return;
    if (categoryRepo.findByName(name).isPresent()) return;
    PlazaCategory created = new PlazaCategory();
    created.setName(name);
    created.setCode(buildCategoryCode(name));
    created.setKeywords(name);
    created.setStatus(GeneralStatus.INACTIVE.name());
    int maxSort = categoryRepo.findAll().stream().mapToInt(PlazaCategory::getSortNo).max().orElse(100);
    created.setSortNo(maxSort + 10);
    categoryRepo.save(created);
  }
}
