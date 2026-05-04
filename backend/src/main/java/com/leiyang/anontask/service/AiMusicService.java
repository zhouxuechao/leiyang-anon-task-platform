package com.leiyang.anontask.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.AiMusicComment;
import com.leiyang.anontask.domain.AiMusicJob;
import com.leiyang.anontask.domain.AiMusicLike;
import com.leiyang.anontask.domain.MusicCreditLedger;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.WalletAccount;
import com.leiyang.anontask.domain.WalletFlow;
import com.leiyang.anontask.domain.enums.FlowStatus;
import com.leiyang.anontask.domain.enums.WalletFlowType;
import com.leiyang.anontask.dto.mp.PlazaCreateRequest;
import com.leiyang.anontask.dto.mp.SliceResponse;
import com.leiyang.anontask.repo.AiMusicJobRepository;
import com.leiyang.anontask.repo.AiMusicCommentRepository;
import com.leiyang.anontask.repo.AiMusicLikeRepository;
import com.leiyang.anontask.repo.MusicCreditLedgerRepository;
import com.leiyang.anontask.repo.WalletAccountRepository;
import com.leiyang.anontask.repo.WalletFlowRepository;
import com.leiyang.anontask.util.Slices;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiMusicService {
  private static final Logger log = LoggerFactory.getLogger(AiMusicService.class);
  private static final String STATUS_SUBMITTED = "SUBMITTED";
  private static final String STATUS_FAILED = "FAILED";
  private static final String GENERATE_LOCK_PREFIX = "music:generate:submit-lock:";
  private static final String REFRESH_LOCK_PREFIX = "music:refresh:poll-lock:";
  private static final ConcurrentHashMap<Long, Instant> LOCAL_GENERATE_LOCKS = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Long, Instant> LOCAL_REFRESH_LOCKS = new ConcurrentHashMap<>();
  private static final List<String> LYRIC_ANGLES = List.of(
      "以具体画面推进叙事，避免抽象口号",
      "从一个细小物件切入，写出情绪转折",
      "用对话感和回忆感交替展开",
      "主歌克制，副歌情绪爆发",
      "用时间、地点和动作形成故事线",
      "写出遗憾里的释然，不要重复常见句式",
      "更口语化，像一个人深夜独白",
      "更电影感，画面要连续推进"
  );

  private final AiMusicJobRepository repo;
  private final AiMusicCommentRepository commentRepo;
  private final AiMusicLikeRepository likeRepo;
  private final ConfigService configService;
  private final ObjectMapper mapper;
  private final HttpClient httpClient;
  private final AiProviderChatService aiProviderChatService;
  private final MpPlazaService plazaService;
  private final WalletService walletService;
  private final MusicCreditLedgerRepository creditLedgerRepo;
  private final WalletAccountRepository walletAccountRepo;
  private final WalletFlowRepository walletFlowRepo;
  private final ObjectProvider<StringRedisTemplate> redisProvider;

  public AiMusicService(
      AiMusicJobRepository repo,
      AiMusicCommentRepository commentRepo,
      AiMusicLikeRepository likeRepo,
      ConfigService configService,
      ObjectMapper mapper,
      AiProviderChatService aiProviderChatService,
      MpPlazaService plazaService,
      WalletService walletService,
      MusicCreditLedgerRepository creditLedgerRepo,
      WalletAccountRepository walletAccountRepo,
      WalletFlowRepository walletFlowRepo,
      ObjectProvider<StringRedisTemplate> redisProvider
  ) {
    this.repo = repo;
    this.commentRepo = commentRepo;
    this.likeRepo = likeRepo;
    this.configService = configService;
    this.mapper = mapper;
    this.aiProviderChatService = aiProviderChatService;
    this.plazaService = plazaService;
    this.walletService = walletService;
    this.creditLedgerRepo = creditLedgerRepo;
    this.walletAccountRepo = walletAccountRepo;
    this.walletFlowRepo = walletFlowRepo;
    this.redisProvider = redisProvider;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
  }

  public record MusicItem(
      long id,
      String title,
      String prompt,
      String style,
      boolean customMode,
      boolean instrumental,
      String lang,
      String status,
      String sunoTaskId,
      String audioUrl,
      String videoUrl,
      String imageUrl,
      String duration,
      boolean published,
      Double rating,
      int ratingCount,
      BigDecimal tipTotal,
      Long plazaPostId,
      String errorMessage,
      String createdAt,
      String updatedAt,
      boolean liked,
      Long authorId,
      String authorName,
      String authorAvatar,
      String lyricist,
      String composer,
      String lyrics
  ) {}

  public record CreditInfo(
      int remaining,
      int total,
      long used,
      boolean freeWeek,
      int dailyFreeRemaining,
      int dailyFreeTotal,
      long packageRemaining,
      BigDecimal paidPrice
  ) {}

  public record MusicPackage(String code, String name, int credits, BigDecimal price, BigDecimal originalPrice, String discountText) {}

  public record GenerationStatus(String status, int percent, String label) {}

  public record MusicCommentItem(long id, long userId, String userName, String userAvatar, String content, String createdAt) {}
  @Transactional
  public List<MusicItem> list(UserAccount user) {
    List<AiMusicJob> jobs = repo.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 30));
    return toItems(jobs, user);
  }

  public CreditInfo credits(UserAccount user) {
    boolean freeWeek = isFreeWeek();
    int dailyTotal = Math.max(0, configService.getInt("music.daily_free", 2));
    long dailyUsed = repo.countByUserAndCreatedAtGreaterThanEqual(user, LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC));
    int dailyRemaining = freeWeek ? dailyTotal : Math.max(0, dailyTotal - (int) dailyUsed);
    long packageRemaining = Math.max(0, creditLedgerRepo.sumByUser(user));
    int total = freeWeek ? 999999 : dailyTotal;
    long used = repo.countByUser(user);
    int remaining = freeWeek ? 999999 : (int) Math.min(Integer.MAX_VALUE, dailyRemaining + packageRemaining);
    return new CreditInfo(remaining, total, used, freeWeek, dailyRemaining, dailyTotal, packageRemaining, musicPaidPrice());
  }

  public List<MusicPackage> packages() {
    return List.of(
        new MusicPackage("starter", "灵感包", 20, new BigDecimal("9.90"), new BigDecimal("12.00"), "8.3折"),
        new MusicPackage("creator", "创作包", 60, new BigDecimal("26.90"), new BigDecimal("36.00"), "7.5折"),
        new MusicPackage("pro", "专业包", 150, new BigDecimal("59.90"), new BigDecimal("90.00"), "6.7折")
    );
  }

  @Transactional
  public MusicPackage buyPackage(UserAccount user, String code) {
    MusicPackage pkg = packages().stream()
        .filter(p -> p.code().equalsIgnoreCase(trim(code)))
        .findFirst()
        .orElseThrow(() -> new BizException("music package not found"));
    walletService.debitForMusic(user, pkg.price(), WalletFlowType.MUSIC_PACKAGE_BUY, "MUSIC_PACKAGE:" + pkg.code());
    MusicCreditLedger ledger = new MusicCreditLedger();
    ledger.setUser(user);
    ledger.setChangeAmount(pkg.credits());
    ledger.setBizType("PACKAGE_BUY");
    ledger.setBizNo(pkg.code());
    ledger.setCreatedAt(Instant.now());
    creditLedgerRepo.save(ledger);
    return pkg;
  }

  public String assist(String type, String title, String prompt, String style, String lang) {
    String action = trim(type).toLowerCase(Locale.ROOT);
    String language = trim(lang).isEmpty() ? "zh" : trim(lang);
    String angle = LYRIC_ANGLES.get(ThreadLocalRandom.current().nextInt(LYRIC_ANGLES.size()));
    String systemPrompt = "你是AI音乐创作助手。只输出用户需要的正文结果，不要解释，不要JSON，不要Markdown。";
    String userPrompt = switch (action) {
      case "lyrics" -> """
          请根据以下信息创作一首适合 Suno 生成歌曲的完整歌词，语言为%s。
          标题：%s
          风格：%s
          创作要求：%s
          创作批次：%s
          本次创作角度：%s
          要求：标题必须成为歌词核心意象或主线；每次都根据标题、风格和创作要求重新创作，不要套用固定模板，不要复用上一版表达；歌词要包含主歌、副歌或桥段等结构，内容不少于12行；只输出歌词正文。
          """.formatted(language, trim(title), trim(style), trim(prompt), Instant.now().toEpochMilli(), angle);
      case "translate" -> """
          请把下面歌词或创作描述翻译为%s，并保留适合歌曲生成的段落感。只输出译文。
          标题：%s
          内容：%s
          """.formatted(language, trim(title), trim(prompt));
      case "style" -> """
          请根据标题和内容补全 Suno 风格标签，输出英文逗号分隔标签，控制在12个标签以内。
          标题：%s
          内容：%s
          当前标签：%s
          目标语言：%s
          """.formatted(trim(title), trim(prompt), trim(style), language);
      default -> throw new BizException("unsupported assist type");
    };
    int maxTokens = "style".equals(action) ? 160 : 900;
    var attempt = aiProviderChatService.chatOnce("deepseek", systemPrompt, userPrompt, maxTokens);
    if (!attempt.ok()) throw new BizException("DeepSeek assist failed: " + attempt.error());
    return cut(cleanAiText(attempt.content()), "style".equals(action) ? 1000 : 5000);
  }

  @Transactional
  public MusicItem create(UserAccount user, String title, String prompt, String style, boolean customMode, boolean instrumental, String lang) {
    String t = cut(required(title, "title is required"), 100);
    String p = cut(required(prompt, "prompt is required"), 5000);
    String s = cut(trim(style), 1000);
    String language = cut(trim(lang), 32);
    ChargeResult charge = checkGenerationCharge(user);
    if (!acquireGenerateSubmitLock(user.getId())) {
      log.info("music_generate_duplicate_blocked userId={}", user.getId());
      throw new BizException("音乐正在提交生成，请勿重复点击");
    }
    String generationPrompt = p;
    boolean generationCustomMode = customMode;
    if (!instrumental && !customMode) {
      generationPrompt = cut(assist("lyrics", p, t, s, language.isEmpty() ? "zh" : language), 5000);
      generationCustomMode = true;
    }

    AiMusicJob job = new AiMusicJob();
    job.setUser(user);
    job.setTitle(t);
    job.setPrompt(generationPrompt);
    job.setStyle(s);
    job.setCustomMode(generationCustomMode);
    job.setInstrumental(instrumental);
    job.setLang(language);
    if (!instrumental) job.setLyrics(generationPrompt);
    job.setStatus(STATUS_SUBMITTED);
    AiMusicJob saved = repo.save(job);
    log.info("music_generate_create userId={} musicId={} title={} customMode={} instrumental={} chargeType={}", user.getId(), saved.getId(), cut(t, 60), generationCustomMode, instrumental, charge.type());

    try {
      JsonNode root = requestGenerate(saved);
      saved.setRawResponse(cut(root.toString(), 12000));
      String taskId = findText(root, "taskId", "task_id", "generation_id", "id");
      if (taskId.isEmpty()) throw new IllegalStateException("Suno API did not return taskId");
      saved.setSunoTaskId(taskId);
      saved.setStatus(firstText(root, "status", "state").isEmpty() ? STATUS_SUBMITTED : firstText(root, "status", "state").toUpperCase(Locale.ROOT));
      chargeForGeneration(user, charge, saved.getId());
      repo.save(saved);
      log.info("music_generate_submitted userId={} musicId={} taskId={} status={}", user.getId(), saved.getId(), saved.getSunoTaskId(), saved.getStatus());
    } catch (Exception e) {
      log.warn("music_generate_submit_failed userId={} musicId={} error={}", user.getId(), saved.getId(), cleanError(e.getMessage()));
      throw new BizException(cleanSunoError(e.getMessage()));
    }
    return toItem(saved);
  }

  private record ChargeResult(String type) {}

  private ChargeResult checkGenerationCharge(UserAccount user) {
    if (isFreeWeek()) return new ChargeResult("FREE_WEEK");
    int dailyTotal = Math.max(0, configService.getInt("music.daily_free", 2));
    long dailyUsed = repo.countByUserAndCreatedAtGreaterThanEqual(user, LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC));
    if (dailyUsed < dailyTotal) return new ChargeResult("DAILY_FREE");
    if (creditLedgerRepo.sumByUser(user) > 0) return new ChargeResult("PACKAGE");
    var wallet = walletService.getOrCreate(user);
    if (wallet.getBalance().compareTo(musicPaidPrice()) < 0) throw new BizException("Insufficient balance");
    return new ChargeResult("PAID");
  }

  private void chargeForGeneration(UserAccount user, ChargeResult charge, long musicId) {
    if ("PACKAGE".equals(charge.type())) {
      MusicCreditLedger ledger = new MusicCreditLedger();
      ledger.setUser(user);
      ledger.setChangeAmount(-1);
      ledger.setBizType("GENERATE_USE");
      ledger.setBizNo(String.valueOf(musicId));
      ledger.setCreatedAt(Instant.now());
      creditLedgerRepo.save(ledger);
    } else if ("PAID".equals(charge.type())) {
      walletService.debitForMusic(user, musicPaidPrice(), WalletFlowType.MUSIC_GENERATE_PAY, "MUSIC_GENERATE:" + musicId);
    }
  }

  private boolean acquireGenerateSubmitLock(Long userId) {
    if (userId == null) return true;
    long ttlSeconds = Math.max(15, configService.getInt("music.submit_lock_seconds", 60));
    try {
      StringRedisTemplate redis = redisProvider == null ? null : redisProvider.getIfAvailable();
      if (redis != null) {
        Boolean ok = redis.opsForValue().setIfAbsent(
            GENERATE_LOCK_PREFIX + userId,
            String.valueOf(Instant.now().toEpochMilli()),
            ttlSeconds,
            TimeUnit.SECONDS
        );
        boolean acquired = Boolean.TRUE.equals(ok);
        log.debug("music_generate_lock userId={} backend=redis acquired={}", userId, acquired);
        return acquired;
      }
    } catch (Exception ignored) {
      // Fall back to the local process lock below.
    }
    Instant now = Instant.now();
    LOCAL_GENERATE_LOCKS.entrySet().removeIf(e -> e.getValue().isBefore(now));
    Instant expiresAt = now.plusSeconds(ttlSeconds);
    boolean acquired = LOCAL_GENERATE_LOCKS.putIfAbsent(userId, expiresAt) == null;
    log.debug("music_generate_lock userId={} backend=local acquired={}", userId, acquired);
    return acquired;
  }

  private boolean isFreeWeek() {
    String raw = configService.getString("music.free_until", "2026-05-07T00:00:00Z").trim();
    try {
      return !raw.isEmpty() && Instant.now().isBefore(Instant.parse(raw));
    } catch (Exception e) {
      return false;
    }
  }

  private BigDecimal musicPaidPrice() {
    String raw = configService.getString("music.paid_price", "1.20").trim();
    try {
      return new BigDecimal(raw);
    } catch (Exception e) {
      return new BigDecimal("1.20");
    }
  }

  @Transactional
  public int enqueuePendingRefreshes() {
    List<String> statuses = List.of(
        STATUS_SUBMITTED,
        "PROCESSING",
        "TEXT",
        "FIRST",
        "GENERATING",
        "RUNNING",
        "PENDING",
        "SUCCESS",
        "COMPLETE",
        "COMPLETED"
    );
    List<AiMusicJob> jobs = repo.findByStatusInAndSunoTaskIdIsNotNullOrderByUpdatedAtAsc(statuses, PageRequest.of(0, 30));
    int count = 0;
    for (AiMusicJob job : jobs) {
      if (!needsRefresh(job)) continue;
      count++;
      refreshJob(job);
    }
    if (count > 0) log.info("music_refresh_pending_compensated count={}", count);
    return count;
  }

  @Transactional
  public MusicItem refresh(UserAccount user, long id) {
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    if (!job.getUser().getId().equals(user.getId())) throw new BizException("music job not found");
    refreshJob(job);
    return toItem(job, user);
  }

  @Transactional
  public MusicItem publish(UserAccount user, long id) {
    AiMusicJob job = requireOwned(user, id);
    if (job.getPublishedAt() == null) job.setPublishedAt(Instant.now());
    job.setPublished(true);
    repo.save(job);
    return toItem(job, user);
  }

  @Transactional
  public MusicItem rename(UserAccount user, long id, String title) {
    AiMusicJob job = requireOwned(user, id);
    job.setTitle(cut(required(title, "title is required"), 100));
    repo.save(job);
    return toItem(job, user);
  }

  @Transactional
  public void delete(UserAccount user, long id) {
    AiMusicJob job = requireOwned(user, id);
    Long plazaPostId = job.getPlazaPostId();
    if (plazaPostId != null) {
      plazaService.markMusicDeleted(plazaPostId);
    }
    commentRepo.deleteByMusic(job);
    likeRepo.deleteByMusic(job);
    repo.delete(job);
  }

  @Transactional
  public MusicItem shareCircle(UserAccount user, long id) {
    AiMusicJob job = requireOwned(user, id);
    if (job.getPlazaPostId() == null) {
      String content = cut("我生成了一首AI歌曲《" + job.getTitle() + "》", 500);
      long postId = plazaService.createPost(user, new PlazaCreateRequest(content, "UNKNOWN", "MUSIC", List.of()));
      job.setPlazaPostId(postId);
    }
    repo.save(job);
    return toItem(job, user);
  }

  @Transactional
  public MusicItem rate(UserAccount user, long id, int stars) {
    if (stars < 1 || stars > 5) throw new BizException("stars must be 1-5");
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    var existing = likeRepo.findByMusicAndUser(job, user);
    if (existing.isPresent()) {
      likeRepo.delete(existing.get());
      job.setRatingTotal(Math.max(0, job.getRatingTotal() - 5));
      job.setRatingCount(Math.max(0, job.getRatingCount() - 1));
    } else {
      AiMusicLike like = new AiMusicLike();
      like.setMusic(job);
      like.setUser(user);
      likeRepo.save(like);
      job.setRatingTotal(job.getRatingTotal() + 5);
      job.setRatingCount(job.getRatingCount() + 1);
    }
    repo.save(job);
    return toItem(job, user);
  }

  @Transactional
  public MusicItem tip(UserAccount from, long id, BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("invalid tip amount");
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    if (job.getUser().getId().equals(from.getId())) throw new BizException("cannot tip yourself");
    WalletAccount payer = walletService.getOrCreate(from);
    if (payer.getBalance().compareTo(amount) < 0) throw new BizException("Insufficient balance");
    WalletAccount receiver = walletService.getOrCreate(job.getUser());
    payer.setBalance(payer.getBalance().subtract(amount));
    receiver.setBalance(receiver.getBalance().add(amount));
    receiver.setTotalIncome(receiver.getTotalIncome().add(amount));
    walletAccountRepo.save(payer);
    walletAccountRepo.save(receiver);
    walletFlowRepo.save(flow(from, WalletFlowType.MUSIC_TIP_OUT, amount, "MUSIC:" + id));
    walletFlowRepo.save(flow(job.getUser(), WalletFlowType.MUSIC_TIP_IN, amount, "MUSIC:" + id));
    job.setTipTotal(nullToZero(job.getTipTotal()).add(amount));
    repo.save(job);
    return toItem(job, from);
  }

  @Transactional
  public MusicItem detail(long id) {
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    return toItem(job);
  }

  @Transactional
  public MusicItem detail(UserAccount viewer, long id) {
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    return toItem(job, viewer);
  }

  @Transactional
  public List<MusicCommentItem> comments(long id) {
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    return commentRepo.findTop50ByMusicOrderByCreatedAtDesc(job).stream()
        .map(c -> new MusicCommentItem(
            c.getId(),
            c.getUser().getId(),
            toDisplayName(c.getUser()),
            trim(c.getUser().getAvatar()),
            c.getContent(),
            c.getCreatedAt().toString()
        ))
        .toList();
  }

  @Transactional
  public long addComment(UserAccount user, long id, String content) {
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    String v = trim(content);
    if (v.isEmpty()) throw new BizException("content is required");
    if (v.length() > 512) throw new BizException("content too long");
    AiMusicComment c = new AiMusicComment();
    c.setMusic(job);
    c.setUser(user);
    c.setContent(v);
    c.setCreatedAt(Instant.now());
    commentRepo.save(c);
    return c.getId();
  }

  @Transactional
  public List<MusicItem> hall(String sort, int page) {
    int safePage = Math.max(1, page);
    PageRequest pr = PageRequest.of(safePage - 1, 20);
    String s = trim(sort).toLowerCase(Locale.ROOT);
    List<AiMusicJob> jobs = "hot".equals(s)
        ? repo.findByPublishedTrueOrderByRatingTotalDescTipTotalDescPublishedAtDesc(pr)
        : repo.findByPublishedTrueOrderByPublishedAtDesc(pr);
    return jobs.stream().map(this::toItem).toList();
  }

  @Transactional
  public SliceResponse<MusicItem> hallSlice(String sort, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 50);
    PageRequest pr = PageRequest.of(safePage - 1, safeSize + 1);
    String s = trim(sort).toLowerCase(Locale.ROOT);
    List<AiMusicJob> jobs = "hot".equals(s)
        ? repo.findByPublishedTrueOrderByRatingTotalDescTipTotalDescPublishedAtDesc(pr)
        : repo.findByPublishedTrueOrderByPublishedAtDesc(pr);
    List<MusicItem> rows = jobs.stream().map(this::toItem).toList();
    return Slices.of(rows, safePage, safeSize);
  }

  @Transactional
  public List<MusicItem> hall(UserAccount viewer, String sort, int page) {
    int safePage = Math.max(1, page);
    PageRequest pr = PageRequest.of(safePage - 1, 20);
    String s = trim(sort).toLowerCase(Locale.ROOT);
    List<AiMusicJob> jobs = "hot".equals(s)
        ? repo.findByPublishedTrueOrderByRatingTotalDescTipTotalDescPublishedAtDesc(pr)
        : repo.findByPublishedTrueOrderByPublishedAtDesc(pr);
    return toItems(jobs, viewer);
  }

  @Transactional
  public SliceResponse<MusicItem> hallSlice(UserAccount viewer, String sort, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 50);
    PageRequest pr = PageRequest.of(safePage - 1, safeSize + 1);
    String s = trim(sort).toLowerCase(Locale.ROOT);
    List<AiMusicJob> jobs = "hot".equals(s)
        ? repo.findByPublishedTrueOrderByRatingTotalDescTipTotalDescPublishedAtDesc(pr)
        : repo.findByPublishedTrueOrderByPublishedAtDesc(pr);
    List<MusicItem> rows = toItems(jobs, viewer);
    return Slices.of(rows, safePage, safeSize);
  }

  @Transactional
  public List<MusicItem> publicUserMusic(long userId, int page, int size) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 50);
    List<AiMusicJob> jobs = repo.findByUserIdAndPublishedTrueOrderByPublishedAtDesc(userId, PageRequest.of(safePage - 1, safeSize));
    return jobs.stream().map(this::toItem).toList();
  }

  @Transactional
  public SliceResponse<MusicItem> publicUserMusicSlice(long userId, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 50);
    List<MusicItem> rows = publicUserMusic(userId, safePage, safeSize + 1);
    return Slices.of(rows, safePage, safeSize);
  }

  @Transactional
  public List<MusicItem> publicUserMusic(UserAccount viewer, long userId, int page, int size) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 50);
    List<AiMusicJob> jobs = repo.findByUserIdAndPublishedTrueOrderByPublishedAtDesc(userId, PageRequest.of(safePage - 1, safeSize));
    return toItems(jobs, viewer);
  }

  @Transactional
  public SliceResponse<MusicItem> publicUserMusicSlice(UserAccount viewer, long userId, int page, int size) {
    int safePage = Slices.safePage(page);
    int safeSize = Slices.safeSize(size, 50);
    List<MusicItem> rows = publicUserMusic(viewer, userId, safePage, safeSize + 1);
    return Slices.of(rows, safePage, safeSize);
  }

  public String generateLyrics(String title, String theme, String style) {
    return generateLyrics(title, theme, style, "");
  }

  public String generateLyrics(String title, String theme, String style, String previousLyrics) {
    String t = trim(title);
    String p = required(theme, "theme is required");
    String previous = trim(previousLyrics);
    if (!previous.isEmpty()) {
      p = p + "\n\n上一版歌词如下，本次必须换新的叙事、意象和副歌，不要复用这些句子：\n" + cut(previous, 1200);
    }
    return assist("lyrics", t.isEmpty() ? p : t, p, trim(style), "zh");
  }

  @Transactional
  public GenerationStatus generationStatus(UserAccount user, long id) {
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    if (!job.getUser().getId().equals(user.getId())) throw new BizException("music job not found");
    return statusOf(job.getStatus(), job.getErrorMessage(), job.getAudioUrl(), job.getDuration());
  }

  @Transactional
  public void handleCallback(JsonNode root) {
    String taskId = findText(root, "taskId", "task_id", "generation_id", "id");
    if (taskId.isEmpty()) taskId = findText(root.path("data"), "taskId", "task_id", "generation_id", "id");
    if (taskId.isEmpty()) {
      log.warn("Suno callback ignored: taskId missing, body={}", cut(root.toString(), 1000));
      return;
    }
    var jobOpt = repo.findBySunoTaskId(taskId);
    if (jobOpt.isEmpty()) {
      log.warn("Suno callback ignored: taskId={} not found", taskId);
      return;
    }
    String callbackTaskId = taskId;
    jobOpt.ifPresent(job -> {
      job.setRawResponse(cut(root.toString(), 12000));
      applyMusicPayload(job, root);
      applyMusicPayload(job, root.path("data"));
      markSuccessWhenPlayable(job);
      fetchTimestampedLyrics(job);
      repo.save(job);
      log.info("Suno callback handled: taskId={}, audioUrl={}, duration={}, status={}", callbackTaskId, !trim(job.getAudioUrl()).isEmpty(), trim(job.getDuration()), trim(job.getStatus()));
    });
  }

  private void refreshJob(AiMusicJob job) {
    if (trim(job.getSunoTaskId()).isEmpty()) return;
    if (!acquireRefreshLock(job.getId())) {
      log.debug("music_refresh_skipped_by_lock musicId={} taskId={}", job.getId(), job.getSunoTaskId());
      return;
    }
    try {
      log.debug("music_refresh_request musicId={} taskId={}", job.getId(), job.getSunoTaskId());
      JsonNode root = requestRecordInfo(job.getSunoTaskId());
      job.setRawResponse(cut(root.toString(), 12000));
      JsonNode data = root.path("data");
      JsonNode node = data.isArray() && !data.isEmpty() ? data.get(0) : (data.isMissingNode() || data.isNull() ? root : data);
      applyMusicPayload(job, node);
      if (firstText(node, "status", "state").isEmpty()) applyMusicPayload(job, root);
      markSuccessWhenPlayable(job);
      fetchTimestampedLyrics(job);
      repo.save(job);
      log.info("music_refresh_success musicId={} taskId={} status={} audioUrl={} duration={} imageUrl={}", job.getId(), job.getSunoTaskId(), job.getStatus(), !trim(job.getAudioUrl()).isEmpty(), trim(job.getDuration()), !trim(job.getImageUrl()).isEmpty());
    } catch (Exception e) {
      job.setErrorMessage(cut(cleanSunoDisplayError(e.getMessage()), 1024));
      repo.save(job);
      log.warn("music_refresh_failed musicId={} taskId={} error={}", job.getId(), job.getSunoTaskId(), cleanError(e.getMessage()));
    }
  }

  private boolean acquireRefreshLock(Long musicId) {
    if (musicId == null) return false;
    int ttl = Math.max(5, configService.getInt("music.refresh_interval_seconds", 20));
    String key = REFRESH_LOCK_PREFIX + musicId;
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) {
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", ttl, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
      }
    } catch (Exception e) {
      log.debug("music_refresh_lock_redis_failed musicId={} error={}", musicId, cleanError(e.getMessage()));
    }
    Instant now = Instant.now();
    LOCAL_REFRESH_LOCKS.entrySet().removeIf(e -> e.getValue().isBefore(now));
    Instant expiresAt = now.plusSeconds(ttl);
    Instant existing = LOCAL_REFRESH_LOCKS.putIfAbsent(musicId, expiresAt);
    if (existing == null || existing.isBefore(now)) {
      LOCAL_REFRESH_LOCKS.put(musicId, expiresAt);
      return true;
    }
    return false;
  }

  private boolean needsRefresh(AiMusicJob job) {
    if (job == null || trim(job.getSunoTaskId()).isEmpty()) return false;
    String status = trim(job.getStatus()).toUpperCase(Locale.ROOT);
    if ("FAILED".equals(status) || "ERROR".equals(status)) return false;
    return trim(job.getAudioUrl()).isEmpty()
        || trim(job.getDuration()).isEmpty()
        || trim(job.getImageUrl()).isEmpty()
        || (!job.isInstrumental() && trim(job.getLyrics()).isEmpty());
  }

  private void applyRecordNode(AiMusicJob job, JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) return;
    String status = normalizeMusicStatus(firstText(node, "status", "state"));
    if (status.isEmpty()) status = normalizeMusicStatus(firstText(node, "callbackType", "callback_type"));
    if (!status.isEmpty()) job.setStatus(status.toUpperCase(Locale.ROOT));
    setIfPresent(job::setAudioUrl, findText(node, "audio_url", "audioUrl", "source_audio_url", "sourceAudioUrl", "stream_audio_url", "streamAudioUrl"));
    setIfPresent(job::setVideoUrl, findText(node, "video_url", "videoUrl"));
    setIfPresent(job::setImageUrl, findText(node, "image_url", "imageUrl", "image_large_url", "imageLargeUrl"));
    setIfPresent(job::setDuration, findText(node, "duration", "duration_s", "durationS", "duration_sec", "durationSec", "audio_duration", "audioDuration"));
    setIfPresent(job::setSunoAudioId, findAudioId(node));
    setIfPresent(job::setLyrics, findText(node, "lyrics", "lyric", "lyric_text", "lyricText", "prompt"));
    String error = findText(node, "errorMessage", "error_message", "errorMsg", "error_msg", "error");
    if (!error.isEmpty()) job.setErrorMessage(cut(cleanSunoDisplayError(error), 1024));
  }

  private void applyMusicPayload(AiMusicJob job, JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) return;
    applyRecordNode(job, node);
    applyResponseNode(job, node.path("response"));
    applySunoDataNode(job, node.path("sunoData"));
    applySunoDataNode(job, node.path("suno_data"));
    applySunoDataNode(job, node.path("data"));
  }

  private void applyResponseNode(AiMusicJob job, JsonNode response) {
    if (response == null || response.isMissingNode() || response.isNull()) return;
    if (response.isTextual()) {
      try {
        applyMusicPayload(job, mapper.readTree(response.asText()));
      } catch (Exception ignored) {
        // Some providers return an object here, some return a serialized JSON string.
      }
    } else {
      applyMusicPayload(job, response);
    }
  }

  private void applySunoDataNode(AiMusicJob job, JsonNode sunoData) {
    if (sunoData == null || sunoData.isMissingNode() || sunoData.isNull()) return;
    JsonNode song = sunoData.isArray() && !sunoData.isEmpty() ? sunoData.get(0) : sunoData;
    applyRecordNode(job, song);
    String tags = findText(song, "tags", "style");
    if (!tags.isEmpty()) job.setStyle(cut(tags, 1000));
    String title = findText(song, "title");
    if (!title.isEmpty() && (trim(job.getTitle()).isEmpty() || "AI歌曲".equals(trim(job.getTitle())))) {
      job.setTitle(cut(title, 100));
    }
    markSuccessWhenPlayable(job);
  }

  private void markSuccessWhenPlayable(AiMusicJob job) {
    if (!trim(job.getAudioUrl()).isEmpty() && !trim(job.getDuration()).isEmpty()) {
      job.setStatus("SUCCESS");
    }
  }

  private void fetchTimestampedLyrics(AiMusicJob job) {
    if (job.isInstrumental()) return;
    if (hasTimestamp(trim(job.getLyrics()))) return;
    String taskId = trim(job.getSunoTaskId());
    String audioId = trim(job.getSunoAudioId());
    if (taskId.isEmpty() || audioId.isEmpty()) return;
    try {
      JsonNode root = requestTimestampedLyrics(taskId, audioId);
      String lrc = toTimestampedLyrics(root);
      if (!lrc.isEmpty()) {
        job.setLyrics(cut(lrc, 12000));
        log.info("music_timestamped_lyrics_saved musicId={} taskId={} audioId={}", job.getId(), taskId, audioId);
      }
    } catch (Exception ignored) {
      log.debug("music_timestamped_lyrics_failed musicId={} taskId={} audioId={}", job.getId(), taskId, audioId);
      // Time-synced lyrics are best-effort; normal music generation should not fail because of them.
    }
  }

  private AiMusicJob requireOwned(UserAccount user, long id) {
    AiMusicJob job = repo.findById(id).orElseThrow(() -> new BizException("music job not found"));
    if (!job.getUser().getId().equals(user.getId())) throw new BizException("music job not found");
    return job;
  }

  private JsonNode requestGenerate(AiMusicJob job) throws Exception {
    String apiKey = required(configService.getString("suno.api.key", ""), "Suno API key not configured");
    String base = stripTrailingSlash(configService.getString("suno.api.base_url", "https://api.sunoapi.org"));
    if (!configService.getBoolean("suno.api.enabled", false)) throw new BizException("Suno API is disabled");
    int timeout = Math.max(5000, configService.getInt("suno.api.timeout_ms", 30000));
    Map<String, Object> payload = new HashMap<>();
    payload.put("customMode", job.isCustomMode());
    payload.put("instrumental", job.isInstrumental());
    payload.put("model", configService.getString("suno.api.model", "V5_5"));
    payload.put("prompt", job.isCustomMode() ? trim(job.getPrompt()) : buildPrompt(job.getPrompt(), job.getLang()));
    if (job.isCustomMode()) {
      payload.put("style", buildStyle(job.getStyle(), job.getLang()));
      payload.put("title", job.getTitle());
    }
    String callbackUrl = buildCallbackUrl();
    payload.put("callBackUrl", callbackUrl);
    log.info("suno_generate_request musicId={} model={} customMode={} instrumental={} callbackUrl={}", job.getId(), payload.get("model"), job.isCustomMode(), job.isInstrumental(), callbackUrl);
    return sendJson("POST", base + "/api/v1/generate", apiKey, payload, timeout);
  }

  private String buildCallbackUrl() {
    String callbackUrl = trim(configService.getString("suno.api.callback_url", ""));
    if (!callbackUrl.isEmpty()) return callbackUrl;
    String publicBase = trim(configService.getString("app.public_base_url", trim(System.getenv("APP_PUBLIC_BASE_URL"))));
    if (publicBase.isEmpty()) publicBase = trim(System.getenv("PUBLIC_BASE_URL"));
    if (publicBase.isEmpty()) publicBase = "http://127.0.0.1:8080";
    return stripTrailingSlash(publicBase) + "/api/public/suno/callback";
  }

  private JsonNode requestRecordInfo(String taskId) throws Exception {
    String apiKey = required(configService.getString("suno.api.key", ""), "Suno API key not configured");
    String base = stripTrailingSlash(configService.getString("suno.api.base_url", "https://api.sunoapi.org"));
    int timeout = Math.max(5000, configService.getInt("suno.api.timeout_ms", 30000));
    return sendJson("GET", base + "/api/v1/generate/record-info?taskId=" + java.net.URLEncoder.encode(taskId, java.nio.charset.StandardCharsets.UTF_8), apiKey, null, timeout);
  }

  private JsonNode requestTimestampedLyrics(String taskId, String audioId) throws Exception {
    String apiKey = required(configService.getString("suno.api.key", ""), "Suno API key not configured");
    String base = stripTrailingSlash(configService.getString("suno.api.base_url", "https://api.sunoapi.org"));
    int timeout = Math.max(5000, configService.getInt("suno.api.timeout_ms", 30000));
    Map<String, Object> payload = new HashMap<>();
    payload.put("taskId", taskId);
    payload.put("audioId", audioId);
    return sendJson("POST", base + "/api/v1/generate/get-timestamped-lyrics", apiKey, payload, timeout);
  }

  private JsonNode sendJson(String method, String url, String apiKey, Map<String, Object> payload, int timeoutMs) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofMillis(timeoutMs))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json");
    if ("POST".equals(method)) builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));
    else builder.GET();
    HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    String body = trim(resp.body());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IllegalStateException("Suno API HTTP " + resp.statusCode() + " " + cut(body, 240));
    }
    JsonNode root = mapper.readTree(body);
    int appCode = root.path("code").asInt(200);
    if (appCode != 200 && appCode != 0) {
      throw new IllegalStateException(firstText(root, "msg", "message", "error"));
    }
    return root;
  }

  private static String cleanSunoError(String message) {
    String msg = cleanSunoDisplayError(message);
    if (msg.startsWith("AI音乐生成服务")) return msg;
    return "AI音乐生成提交失败：" + msg;
  }

  private static String cleanSunoDisplayError(String message) {
    String msg = cleanError(message);
    String lower = msg.toLowerCase(Locale.ROOT);
    if (lower.contains("current credits are insufficient") || lower.contains("please top up") || lower.contains("insufficient")) {
      return "AI音乐生成服务额度不足，请联系平台管理员充值后再试";
    }
    if (lower.contains("call frequency is too high")
        || lower.contains("frequency")
        || lower.contains("too many requests")
        || lower.contains("rate limit")
        || lower.contains("429")) {
      return "AI音乐生成服务调用过于频繁，请稍后再试";
    }
    return msg;
  }

  private MusicItem toItem(AiMusicJob j) {
    return toItem(j, null, false);
  }

  private MusicItem toItem(AiMusicJob j, UserAccount viewer) {
    boolean liked = viewer != null && likeRepo.findByMusicAndUser(j, viewer).isPresent();
    return toItem(j, viewer, liked);
  }

  private List<MusicItem> toItems(List<AiMusicJob> jobs, UserAccount viewer) {
    if (viewer == null || jobs.isEmpty()) return jobs.stream().map(this::toItem).toList();
    var likedIds = likeRepo.findByUserAndMusicIn(viewer, jobs).stream()
        .map(l -> l.getMusic().getId())
        .collect(Collectors.toSet());
    return jobs.stream().map(j -> toItem(j, viewer, likedIds.contains(j.getId()))).toList();
  }

  private MusicItem toItem(AiMusicJob j, UserAccount viewer, boolean liked) {
    return new MusicItem(
        j.getId(),
        j.getTitle(),
        j.getPrompt(),
        j.getStyle(),
        j.isCustomMode(),
        j.isInstrumental(),
        j.getLang(),
        j.getStatus(),
        j.getSunoTaskId(),
        j.getAudioUrl(),
        j.getVideoUrl(),
        j.getImageUrl(),
        j.getDuration(),
        j.isPublished(),
        j.getRatingCount() <= 0 ? null : ((double) j.getRatingTotal()) / j.getRatingCount(),
        j.getRatingCount(),
        nullToZero(j.getTipTotal()),
        j.getPlazaPostId(),
        j.getErrorMessage(),
        j.getCreatedAt().toString(),
        j.getUpdatedAt().toString(),
        liked,
        j.getUser().getId(),
        displayName(j.getUser()),
        trim(j.getUser().getAvatar()),
        lyricistOf(j),
        "AI音乐生成",
        lyricsOf(j)
    );
  }

  private static String lyricistOf(AiMusicJob j) {
    if (j.isInstrumental()) return "纯音乐";
    return j.isCustomMode() ? displayName(j.getUser()) : "AI作词";
  }

  private static String lyricsOf(AiMusicJob j) {
    if (j.isInstrumental()) return "";
    String lyrics = trim(j.getLyrics());
    if (!lyrics.isEmpty()) return lyrics;
    return j.isCustomMode() ? trim(j.getPrompt()) : "";
  }

  private static String displayName(UserAccount user) {
    if (user == null) return "匿名用户";
    String n = trim(user.getNickname());
    return n.isEmpty() ? "用户" + user.getId() : n;
  }

  private static GenerationStatus statusOf(String status, String error, String audioUrl, String duration) {
    String s = trim(status).toUpperCase(Locale.ROOT);
    String displayError = cleanSunoDisplayError(error);
    if (!displayError.isEmpty() && "FAILED".equals(s)) return new GenerationStatus("failed", 100, displayError);
    boolean hasAudio = !trim(audioUrl).isEmpty();
    boolean hasDuration = !trim(duration).isEmpty();
    if (hasAudio && hasDuration) return new GenerationStatus("completed", 100, "已完成");
    if (hasAudio) return new GenerationStatus("processing", 96, "音频已生成，正在补齐时长");
    if ("SUCCESS".equals(s) || "COMPLETE".equals(s) || "COMPLETED".equals(s)) return new GenerationStatus("processing", 92, "生成完成，正在同步音频信息");
    if ("FAILED".equals(s) || "ERROR".equals(s)) return new GenerationStatus("failed", 100, displayError.isEmpty() ? "生成失败" : displayError);
    return new GenerationStatus("processing", 45, "生成中");
  }

  private static WalletFlow flow(UserAccount user, WalletFlowType type, BigDecimal amount, String bizNo) {
    WalletFlow f = new WalletFlow();
    f.setUser(user);
    f.setFlowType(type);
    f.setAmount(amount);
    f.setBizNo(bizNo);
    f.setStatus(FlowStatus.SUCCESS);
    f.setCreatedAt(Instant.now());
    return f;
  }

  private static BigDecimal nullToZero(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private static List<String> imageList(String imageUrl) {
    String v = trim(imageUrl);
    return v.isEmpty() ? List.of() : List.of(v);
  }

  private static String buildPrompt(String prompt, String lang) {
    String language = trim(lang);
    if (language.isEmpty()) return trim(prompt);
    return "语言：" + language + "\n" + trim(prompt);
  }

  private static String buildStyle(String style, String lang) {
    String s = trim(style);
    String language = trim(lang);
    if (language.isEmpty()) return s;
    if (s.toLowerCase(Locale.ROOT).contains(language.toLowerCase(Locale.ROOT))) return s;
    return s.isEmpty() ? language : s + ", " + language;
  }

  private static String firstText(JsonNode node, String... names) {
    for (String name : names) {
      String v = trim(node.path(name).asText(""));
      if (!v.isEmpty()) return v;
    }
    return "";
  }

  private static String normalizeMusicStatus(String value) {
    String v = trim(value).toUpperCase(Locale.ROOT);
    return switch (v) {
      case "COMPLETE", "COMPLETED", "SUCCESS" -> "SUCCESS";
      case "ERROR", "FAILED", "FAILURE" -> "FAILED";
      case "TEXT", "FIRST", "GENERATING", "PROCESSING", "RUNNING", "PENDING", "SUBMITTED" -> "PROCESSING";
      default -> v;
    };
  }

  private static String findText(JsonNode node, String... names) {
    if (node == null || node.isMissingNode() || node.isNull()) return "";
    String direct = firstText(node, names);
    if (!direct.isEmpty()) return direct;
    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        String v = findText(it.next().getValue(), names);
        if (!v.isEmpty()) return v;
      }
    } else if (node.isArray()) {
      for (JsonNode child : node) {
        String v = findText(child, names);
        if (!v.isEmpty()) return v;
      }
    }
    return "";
  }

  private static String findAudioId(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) return "";
    if (node.isObject()) {
      String audioUrl = firstText(node, "audio_url", "audioUrl", "source_audio_url", "sourceAudioUrl", "stream_audio_url", "streamAudioUrl");
      String id = firstText(node, "id", "audioId", "audio_id", "clipId", "clip_id");
      if (!audioUrl.isEmpty() && !id.isEmpty()) return id;
      Iterator<Map.Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        String v = findAudioId(it.next().getValue());
        if (!v.isEmpty()) return v;
      }
    } else if (node.isArray()) {
      for (JsonNode child : node) {
        String v = findAudioId(child);
        if (!v.isEmpty()) return v;
      }
    }
    return "";
  }

  private static boolean hasTimestamp(String text) {
    return text.matches("(?s).*\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?].*");
  }

  private static String toTimestampedLyrics(JsonNode root) {
    JsonNode words = findNode(root, "alignedWords");
    if (words == null || !words.isArray() || words.isEmpty()) return "";
    List<String> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    double lineStart = -1;
    double previousStart = -1;
    for (JsonNode wordNode : words) {
      String word = trim(firstText(wordNode, "word", "text"));
      double start = wordNode.path("startS").asDouble(wordNode.path("start").asDouble(-1));
      if (word.isEmpty() || start < 0) continue;
      String[] parts = word.replace("\r", "").split("\n", -1);
      for (int i = 0; i < parts.length; i += 1) {
        String part = trim(parts[i]);
        if (!part.isEmpty()) {
          if (lineStart < 0) lineStart = start;
          boolean gap = previousStart >= 0 && start - previousStart > 1.4;
          boolean longLine = current.length() >= 22 && part.matches(".*[，。！？,.!?]$");
          if ((gap || longLine) && current.length() > 0) {
            lines.add(formatLrcTime(lineStart) + current.toString().trim());
            current.setLength(0);
            lineStart = start;
          }
          if (current.length() > 0 && !part.matches("^[，。！？、,.!?].*")) current.append(' ');
          current.append(part);
          previousStart = start;
        }
        if (i < parts.length - 1 && current.length() > 0) {
          lines.add(formatLrcTime(lineStart) + current.toString().trim());
          current.setLength(0);
          lineStart = -1;
        }
      }
    }
    if (current.length() > 0) lines.add(formatLrcTime(lineStart) + current.toString().trim());
    return String.join("\n", lines);
  }

  private static JsonNode findNode(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.isNull()) return null;
    if (node.has(fieldName)) return node.get(fieldName);
    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        JsonNode found = findNode(it.next().getValue(), fieldName);
        if (found != null) return found;
      }
    } else if (node.isArray()) {
      for (JsonNode child : node) {
        JsonNode found = findNode(child, fieldName);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static String formatLrcTime(double seconds) {
    int total = Math.max(0, (int) Math.floor(seconds));
    int minutes = total / 60;
    int secs = total % 60;
    int centis = Math.max(0, Math.min(99, (int) Math.floor((seconds - Math.floor(seconds)) * 100)));
    return "[%02d:%02d.%02d]".formatted(minutes, secs, centis);
  }

  private static void setIfPresent(java.util.function.Consumer<String> setter, String value) {
    String v = trim(value);
    if (!v.isEmpty()) setter.accept(v);
  }

  private static String required(String value, String message) {
    String v = trim(value);
    if (v.isEmpty()) throw new BizException(message);
    return v;
  }

  private static String stripTrailingSlash(String s) {
    String v = trim(s);
    while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
    return v;
  }

  private static String cleanError(String s) {
    String v = trim(s);
    return v.isEmpty() ? "unknown error" : v;
  }

  private static String cleanAiText(String s) {
    String v = trim(s);
    if (v.startsWith("```")) {
      v = v.replaceFirst("^```[a-zA-Z]*\\s*", "");
      if (v.endsWith("```")) v = v.substring(0, v.length() - 3);
    }
    return trim(v);
  }

  private static String cut(String s, int max) {
    String v = trim(s);
    return v.length() > max ? v.substring(0, max) : v;
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private static String toDisplayName(UserAccount user) {
    String n = trim(user.getNickname());
    if (!n.isEmpty()) return n;
    String openId = trim(user.getOpenId());
    return openId.length() <= 8 ? openId : openId.substring(0, 8);
  }
}
