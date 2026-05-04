package com.leiyang.anontask.service;

import com.leiyang.anontask.domain.AiCommentJob;
import com.leiyang.anontask.domain.PlazaAiProvider;
import com.leiyang.anontask.domain.PlazaPost;
import com.leiyang.anontask.domain.PlazaPostComment;
import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.enums.SensitiveWordAction;
import com.leiyang.anontask.domain.enums.TaskStatus;
import com.leiyang.anontask.domain.enums.UserStatus;
import com.leiyang.anontask.repo.AiCommentJobRepository;
import com.leiyang.anontask.repo.PlazaAiProviderRepository;
import com.leiyang.anontask.repo.PlazaPostRepository;
import com.leiyang.anontask.repo.PlazaPostCommentRepository;
import com.leiyang.anontask.repo.TaskPublishRepository;
import com.leiyang.anontask.repo.UserAccountRepository;
import com.leiyang.anontask.util.NoGenerator;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.config.AsyncMessagingConfig;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiAutomationService {
  private static final Logger log = LoggerFactory.getLogger(AiAutomationService.class);
  private static final String BOT_OPENID_PREFIX = "ai-bot:";
  private static final String JOB_PENDING = "PENDING";
  private static final String JOB_SUCCESS = "SUCCESS";
  private static final String JOB_FAILED = "FAILED";
  private static final int MIN_COMMENT_LEN = 30;
  private static final int COMMENT_WORKERS = 1;

  private final AiCommentJobRepository commentJobRepo;
  private final PlazaAiProviderRepository providerRepo;
  private final PlazaPostRepository postRepo;
  private final UserAccountRepository userRepo;
  private final PlazaPostCommentRepository commentRepo;
  private final TaskPublishRepository taskRepo;
  private final SensitiveWordService sensitiveWordService;
  private final AiProviderChatService aiProviderChatService;
  private final ConfigService configService;
  private final MessageQueueService queueService;
  private final ExecutorService commentExecutor = Executors.newFixedThreadPool(COMMENT_WORKERS);
  private final AtomicBoolean commentConsuming = new AtomicBoolean(false);

  public AiAutomationService(
      AiCommentJobRepository commentJobRepo,
      PlazaAiProviderRepository providerRepo,
      PlazaPostRepository postRepo,
      UserAccountRepository userRepo,
      PlazaPostCommentRepository commentRepo,
      TaskPublishRepository taskRepo,
      SensitiveWordService sensitiveWordService,
      AiProviderChatService aiProviderChatService,
      ConfigService configService,
      MessageQueueService queueService
  ) {
    this.commentJobRepo = commentJobRepo;
    this.providerRepo = providerRepo;
    this.postRepo = postRepo;
    this.userRepo = userRepo;
    this.commentRepo = commentRepo;
    this.taskRepo = taskRepo;
    this.sensitiveWordService = sensitiveWordService;
    this.aiProviderChatService = aiProviderChatService;
    this.configService = configService;
    this.queueService = queueService;
  }

  public record AiCommentConsumeEvent(String reason, Instant createdAt) {}

  @Transactional
  public void autoCommentForNewPost(PlazaPost post) {
    if (post == null || post.getAuthor() == null) return;
    if (!configService.getBoolean("ai.auto_comment.enabled", true)) return;
    if (isAiBot(post.getAuthor())) return;

    int maxProviders = Math.max(1, configService.getInt("ai.auto_comment.max_providers", 10));
    String status = "ACTIVE";
    List<PlazaAiProvider> providers = providerRepo.findByStatusOrderBySortNoAscIdAsc(status).stream()
        .limit(maxProviders)
        .toList();
    if (providers.isEmpty()) return;

    for (PlazaAiProvider provider : providers) {
      String code = norm(provider.getCode());
      if (code.isEmpty()) continue;
      if (!isProviderReadyForComment(code)) continue;
      if (commentJobRepo.findByPostIdAndProviderCode(post.getId(), code).isPresent()) continue;
      AiCommentJob job = new AiCommentJob();
      job.setPost(post);
      job.setProviderCode(code);
      job.setStatus(JOB_PENDING);
      job.setAttempts(0);
      job.setNextRetryAt(Instant.now());
      commentJobRepo.save(job);
    }
    log.info("ai_comment_jobs_created postId={} providerCount={}", post.getId(), providers.size());
    publishCommentConsume("new-post");
  }

  @Transactional
  public int processPendingCommentJobs() {
    if (!configService.getBoolean("ai.auto_comment.enabled", true)) return 0;
    if (!commentConsuming.compareAndSet(false, true)) return 0;
    int batchSize = Math.max(1, configService.getInt("ai.auto_comment.batch_size", 20));
    try {
      List<AiCommentJob> jobs = commentJobRepo.findTop100ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
          JOB_PENDING,
          Instant.now()
      );
      List<Long> ids = jobs.stream().limit(batchSize).map(AiCommentJob::getId).toList();
      if (ids.isEmpty()) return 0;
      log.info("ai_comment_consume_start batchSize={} jobIds={}", ids.size(), ids);
      List<CompletableFuture<Void>> tasks = ids.stream()
          .map(id -> CompletableFuture.runAsync(() -> processOneCommentJobById(id), commentExecutor)
              .exceptionally(ex -> null))
          .toList();
      CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
      log.info("ai_comment_consume_done batchSize={}", ids.size());
      return ids.size();
    } finally {
      commentConsuming.set(false);
    }
  }

  @Transactional
  public void retryCommentJob(long jobId) {
    AiCommentJob job = commentJobRepo.findById(jobId).orElseThrow(() -> new BizException("Job not found"));
    job.setStatus(JOB_PENDING);
    job.setNextRetryAt(Instant.now());
    job.setLastError(null);
    job.setProcessedAt(null);
    job.setCommentId(null);
    commentJobRepo.save(job);
    log.info("ai_comment_job_retry jobId={} postId={} provider={}", jobId, job.getPost() == null ? null : job.getPost().getId(), job.getProviderCode());
    publishCommentConsume("retry");
  }

  @Transactional
  public void deleteCommentJob(long jobId) {
    if (!commentJobRepo.existsById(jobId)) throw new BizException("Job not found");
    commentJobRepo.deleteById(jobId);
  }

  public int triggerCommentConsumeOnce() {
    return processPendingCommentJobs();
  }

  private void safeConsumePendingJobs() {
    try {
      int rounds = 0;
      while (rounds < 10) {
        int done = processPendingCommentJobs();
        if (done <= 0) break;
        rounds++;
      }
    } catch (Exception ignore) {
      // no-op, queued jobs will be retried on next publish trigger
    }
  }

  private void publishCommentConsume(String reason) {
    if (!queueService.publish(
        AsyncMessagingConfig.AI_COMMENT_ROUTING_KEY,
        new AiCommentConsumeEvent(reason, Instant.now())
    )) {
      CompletableFuture.runAsync(this::safeConsumePendingJobs);
    }
  }

  private void processOneCommentJobById(Long id) {
    if (id == null) return;
    AiCommentJob job = commentJobRepo.findById(id).orElse(null);
    if (job == null) return;
    processOneCommentJob(job);
  }

  private void processOneCommentJob(AiCommentJob job) {
    Instant now = Instant.now();
    if (job.getNextRetryAt() != null && job.getNextRetryAt().isAfter(now)) return;
    if (!JOB_PENDING.equalsIgnoreCase(job.getStatus())) return;

    PlazaPost post = job.getPost();
    if (post == null || post.getAuthor() == null) {
      markFailed(job, "post not found", true);
      return;
    }
    if (isAiBot(post.getAuthor())) {
      markSuccess(job, null);
      return;
    }

    PlazaAiProvider provider = providerRepo.findFirstByCodeIgnoreCase(job.getProviderCode()).orElse(null);
    if (provider == null) {
      markFailed(job, "provider missing: " + job.getProviderCode(), true);
      return;
    }
    if (!isProviderReadyForComment(norm(provider.getCode()))) {
      markFailed(job, "provider not configured: " + norm(provider.getCode()), true);
      return;
    }

    try {
      UserAccount bot = ensureBotUser(provider);
      if (commentRepo.existsByPostAndUser(post, bot)) {
        markSuccess(job, null);
        return;
      }

      String comment = generateAutoComment(provider, post);
      String finalComment = cut300(cleanLine(comment));
      if (isInvalidAiComment(finalComment)) throw new IllegalStateException("invalid ai comment");
      if (sensitiveWordService.evaluate(finalComment) == SensitiveWordAction.REJECT) {
        throw new IllegalStateException("ai comment rejected by sensitive checker");
      }
      finalComment = ensureMinCommentLength(finalComment);

      PlazaPostComment c = new PlazaPostComment();
      c.setPost(post);
      c.setUser(bot);
      c.setContent(finalComment);
      commentRepo.save(c);
      post.setCommentCount((int) commentRepo.countByPost(post));
      postRepo.save(post);
      log.info("ai_comment_created jobId={} postId={} provider={} commentId={}", job.getId(), post.getId(), provider.getCode(), c.getId());
      markSuccess(job, c.getId());
    } catch (Exception e) {
      String msg = safe(e.getMessage());
      log.warn("ai_comment_failed jobId={} postId={} provider={} error={}", job.getId(), post.getId(), job.getProviderCode(), msg);
      boolean permanent = msg.contains("invalid ai comment")
          || msg.contains("empty ai comment")
          || msg.contains("ai comment rejected");
      markFailed(job, msg, permanent);
    }
  }

  private void markSuccess(AiCommentJob job, Long commentId) {
    job.setStatus(JOB_SUCCESS);
    job.setCommentId(commentId);
    job.setProcessedAt(Instant.now());
    job.setLastError(null);
    commentJobRepo.save(job);
    log.info("ai_comment_job_success jobId={} commentId={}", job.getId(), commentId);
  }

  private void markFailed(AiCommentJob job, String msg, boolean permanent) {
    int maxRetry = Math.max(0, configService.getInt("ai.auto_comment.max_retry", 3));
    int baseDelaySec = Math.max(3, configService.getInt("ai.auto_comment.retry_delay_sec", 15));
    int nextAttempts = job.getAttempts() + 1;
    job.setAttempts(nextAttempts);
    job.setLastError(cutLen(nonBlank(msg, "unknown"), 500));
    if (permanent || nextAttempts > maxRetry) {
      job.setStatus(JOB_FAILED);
      job.setProcessedAt(Instant.now());
    } else {
      long delay = (long) baseDelaySec * (1L << Math.min(6, nextAttempts - 1));
      job.setStatus(JOB_PENDING);
      job.setNextRetryAt(Instant.now().plusSeconds(Math.min(delay, 900)));
    }
    commentJobRepo.save(job);
    log.info("ai_comment_job_mark_failed jobId={} status={} attempts={} permanent={} error={}", job.getId(), job.getStatus(), job.getAttempts(), permanent, job.getLastError());
  }

  @Transactional
  public Optional<String> autoPublishAiTaskOnce() {
    if (!configService.getBoolean("ai.auto_task.enabled", false)) return Optional.empty();
    if (taskRepo.countByStatus(TaskStatus.PUBLISHED) >= Math.max(20, configService.getInt("ai.auto_task.max_live_tasks", 200))) {
      return Optional.empty();
    }

    List<PlazaAiProvider> providers = providerRepo.findByStatusOrderBySortNoAscIdAsc("ACTIVE");
    if (providers.isEmpty()) return Optional.empty();
    PlazaAiProvider provider = providers.get(ThreadLocalRandom.current().nextInt(providers.size()));
    UserAccount bot = ensureBotUser(provider);

    TaskDraft draft = generateAutoTaskDraft(provider);
    TaskPublish t = new TaskPublish();
    t.setTaskNo(NoGenerator.gen("T"));
    t.setPublisher(bot);
    t.setTitle(cutLen(withAiTaskPrefix(provider, draft.title()), 120));
    t.setContent(cutLen(draft.content(), 1000));
    t.setCategory(cutLen(nonBlank(draft.category(), "AI灵感"), 32));
    t.setLocationText("线上");
    t.setAmount(draft.amount().min(BigDecimal.valueOf(100)).max(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP));
    t.setTotalSlots(Math.max(1, configService.getInt("ai.auto_task.total_slots", 9999)));
    t.setAcceptedSlots(0);
    t.setDeadlineAt(Instant.now().plus(Math.max(2, configService.getInt("ai.auto_task.deadline_hours", 24)), ChronoUnit.HOURS));
    t.setProofRequirements("文字说明即可，图片可选（最多4张）");
    t.setStatus(TaskStatus.PUBLISHED);
    taskRepo.save(t);
    log.info("ai_task_auto_published taskNo={} provider={} amount={}", t.getTaskNo(), provider.getCode(), t.getAmount());
    return Optional.of(t.getTaskNo());
  }

  private String generateAutoComment(PlazaAiProvider provider, PlazaPost post) {
    String providerCode = norm(provider.getCode());
    boolean plainTextMode = isPlainTextCommentProvider(providerCode);
    String systemPrompt = plainTextMode ? """
        你是一个友好、克制的社区AI助手。
        基于帖子文字生成1条中文评论，30-80字。
        只输出最终评论正文，不要输出JSON，不要输出字段名，不要输出“评论内容”这种占位词。
        不要复述提示词，不要输出“用户要求”“帖子内容”“我的回答”等说明。
        不要分析，不要列步骤，不要输出思考过程。
        不要使用营销话术，不要使用emoji，不要换行。
        """ : """
        你是一个友好、克制的社区AI助手。
        基于帖子文字生成1条中文评论，30-80字。
        不要复述提示词，不要输出“用户要求”“帖子内容”“我的回答”等说明。
        不要分析，不要列步骤，不要输出思考过程，只输出最终评论。
        不要使用营销话术，不要使用emoji，不要换行。
        只返回JSON对象，字段为comment，comment必须是真实评论内容。
        """;
    String userPrompt = """
        帖子分类：%s
        帖子内容：%s

        请结合帖子内容，像真实社区用户一样自然回应。不要照抄示例，不要输出“评论内容”。
        """.formatted(safe(post.getCategory()), safe(post.getContent()));
    var attempt = aiProviderChatService.chatOnce(providerCode, systemPrompt, userPrompt, 320);
    if (!attempt.ok()) throw new IllegalStateException(attempt.error());
    String comment = sanitizeAiComment(attempt.content());
    if (!isInvalidAiComment(comment)) return comment;

    String retrySystem = """
        你只负责写一条中文社区评论。
        直接输出评论正文，30-80字，不要JSON，不要字段名，不要解释，不要输出“评论内容”。
        """;
    String retryPrompt = """
        帖子分类：%s
        帖子内容：%s

        上一次输出无效。请重新写一条具体、自然、有内容的评论，只返回评论正文。
        """.formatted(safe(post.getCategory()), safe(post.getContent()));
    var retry = aiProviderChatService.chatOnce(providerCode, retrySystem, retryPrompt, 320);
    if (!retry.ok()) throw new IllegalStateException(retry.error());
    return sanitizeAiComment(retry.content());
  }

  private static boolean isPlainTextCommentProvider(String providerCode) {
    String code = norm(providerCode);
    return code.contains("mimo") || code.contains("xiaomi") || code.contains("mi-mo");
  }

  private static String sanitizeAiComment(String raw) {
    String text = cleanLine(raw);
    String json = extractJson(text);
    if (!json.isEmpty()) {
      try {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String comment = cleanLine(mapper.readTree(json).path("comment").asText(""));
        if (!comment.isBlank()) text = comment;
      } catch (Exception ignore) {
        // fall back to text cleanup below
      }
    }
    if (text.contains("\"comment\"")) {
      var m = java.util.regex.Pattern.compile("\"comment\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
      String last = "";
      while (m.find()) last = cleanLine(m.group(1));
      if (!last.isBlank()) text = last;
    }
    if (text.contains("comment")) {
      var m = java.util.regex.Pattern.compile("(?i)\\{?\\s*comment\\s*[:=]\\s*[\"“]([^\"”]+)[\"”]\\s*}?", java.util.regex.Pattern.DOTALL).matcher(text);
      String last = "";
      while (m.find()) last = cleanLine(m.group(1));
      if (!last.isBlank()) text = last;
    }
    text = text.replaceFirst("^\\s*\\{\\s*comment\\s*[:=]\\s*", "").trim();
    text = text.replaceFirst("\\s*}\\s*$", "").trim();
    text = text.replaceFirst("^\\s*(?:用户要求|帖子内容|我的回答|回答|评论)\\s*[：:]\\s*", "").trim();
    text = text.replaceFirst("^\\s*根据(?:你|用户)?(?:的)?帖子(?:内容)?\\s*[，,：:]\\s*", "").trim();
    text = text.replaceFirst("^\\s*这是一条(?:自然)?评论\\s*[：:]\\s*", "").trim();
    if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("“") && text.endsWith("”"))) {
      text = text.substring(1, text.length() - 1).trim();
    }
    return cleanLine(text);
  }

  private static boolean isInvalidAiComment(String text) {
    String v = cleanLine(text);
    if (v.isEmpty()) return true;
    String compact = v.replaceAll("\\s+", "");
    if (compact.matches("[.。…·,，!！?？~～_-]+")) return true;
    if (compact.equals("...") || compact.equals("…") || compact.equals("。。")) return true;
    if (compact.equals("评论内容") || compact.equals("真实评论内容") || compact.equals("最终评论") || compact.equals("最终评论正文")) return true;
    if (compact.matches("(?i)^(comment|content|reply)$")) return true;
    if (compact.contains("{\"comment\":\"...\"}") || compact.contains("\"comment\":\"...\"")) return true;
    if (compact.contains("\"comment\":\"评论内容\"") || compact.contains("\"comment\":\"真实评论内容\"")) return true;
    if (v.contains("分析请求") || v.contains("分析输入") || v.contains("角色：") || v.contains("任务：") || v.contains("长度限制")) return true;
    if (v.contains("**") || v.matches("^\\d+[\\.、].*")) return true;
    if (v.length() > 120) return true;
    return false;
  }

  private boolean isProviderReadyForComment(String providerCode) {
    return aiProviderChatService.isReady(providerCode);
  }

  private TaskDraft generateAutoTaskDraft(PlazaAiProvider provider) {
    String systemPrompt = """
        你要生成一个任务平台可发布的轻任务。
        要求：
        1) 主题偏“提问+观点收集”，例如“什么是爱情”
        2) title 12-28字
        3) content 40-180字
        4) amount 1-100 之间
        只返回JSON：{"title":"...","content":"...","amount":12.5,"category":"..."}
        """;
    String userPrompt = "请生成一个今天可以发布的任务。";
    Optional<String> resp = aiProviderChatService.chat(norm(provider.getCode()), systemPrompt, userPrompt, 220);
    if (resp.isPresent()) {
      TaskDraft parsed = parseDraft(resp.get());
      if (!parsed.title().isBlank() && !parsed.content().isBlank()) return parsed;
    }
    String[] titles = {
        "AI想知道：什么是爱情",
        "AI提问：成年人最难的选择是什么",
        "AI话题：你最想重来的一个决定",
        "AI调查：你认为什么才是稳定感"
    };
    String title = titles[ThreadLocalRandom.current().nextInt(titles.length)];
    String content = "请分享你最真实的看法，文字即可，也可以补充图片例子。我们会综合点赞和观点质量做展示。";
    BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(9, 99)).setScale(2, RoundingMode.HALF_UP);
    return new TaskDraft(title, content, amount, "AI灵感");
  }

  private static String withAiTaskPrefix(PlazaAiProvider provider, String title) {
    String t = stripAiTaskPrefix(safe(title));
    String name = safe(provider == null ? "" : provider.getName()).trim();
    if (name.isBlank()) name = safe(provider == null ? "" : provider.getCode()).trim();
    if (name.isBlank()) name = "AI";
    String prefix = name + " AI想知道：";
    return t.isBlank() ? prefix : prefix + t;
  }

  private static String stripAiTaskPrefix(String value) {
    String t = safe(value).trim();
    if (t.isBlank()) return "";
    t = t.replaceFirst("^\\s*[\\p{L}\\p{IsHan}0-9_\\- ]+\\s+AI想知道\\s*[：:]\\s*", "");
    t = t.replaceFirst("^\\s*(?:Ai|AI)想知道\\s*[：:]\\s*", "");
    t = t.replaceFirst("^\\s*(?:Ai|AI)想问\\s*[：:]\\s*", "");
    t = t.replaceFirst("^\\s*(?:Ai|AI)提问\\s*[：:]\\s*", "");
    return t.trim();
  }

  private TaskDraft parseDraft(String raw) {
    String json = extractJson(raw);
    if (json.isEmpty()) return new TaskDraft("", "", BigDecimal.valueOf(9.9), "AI灵感");
    try {
      var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      var node = mapper.readTree(json);
      String title = cleanLine(node.path("title").asText(""));
      String content = cleanLine(node.path("content").asText(""));
      BigDecimal amount = BigDecimal.valueOf(node.path("amount").asDouble(9.9));
      String category = cleanLine(node.path("category").asText("AI灵感"));
      return new TaskDraft(title, content, amount, category);
    } catch (Exception ignore) {
      return new TaskDraft("", "", BigDecimal.valueOf(9.9), "AI灵感");
    }
  }

  private UserAccount ensureBotUser(PlazaAiProvider provider) {
    String code = norm(provider.getCode());
    String openId = BOT_OPENID_PREFIX + code;
    UserAccount u = userRepo.findByOpenId(openId).orElseGet(() -> {
      UserAccount created = new UserAccount();
      created.setOpenId(openId);
      created.setNickname(provider.getName() + " AI");
      created.setAvatar(safe(provider.getLogoUrl()));
      created.setGender("UNKNOWN");
      created.setSignature("AI自动助手");
      created.setStatus(UserStatus.ACTIVE);
      created.setCreditScore(100);
      return userRepo.save(created);
    });
    String logoUrl = safe(provider.getLogoUrl());
    if (!logoUrl.equals(safe(u.getAvatar()))) {
      u.setAvatar(logoUrl);
      u = userRepo.save(u);
    }
    String nick = provider.getName() + " AI";
    if (!nick.equals(safe(u.getNickname()))) {
      u.setNickname(nick);
      u = userRepo.save(u);
    }
    return u;
  }

  private static boolean isAiBot(UserAccount u) {
    return u.getOpenId() != null && u.getOpenId().startsWith(BOT_OPENID_PREFIX);
  }

  private static String ensureMinCommentLength(String text) {
    String v = cut300(cleanLine(text));
    return v;
  }

  private static String extractFirstLine(String s) {
    return cleanLine(s).split("[。!?！？]")[0] + "。";
  }

  private static String extractJson(String text) {
    int start = text.indexOf('{');
    int end = text.lastIndexOf('}');
    if (start < 0 || end <= start) return "";
    return text.substring(start, end + 1);
  }

  private static String cut300(String s) {
    String v = safe(s);
    return v.length() <= 300 ? v : v.substring(0, 300);
  }

  private static String cutLen(String s, int max) {
    String v = safe(s);
    return v.length() <= max ? v : v.substring(0, max);
  }

  private static String cleanLine(String s) {
    return safe(s).replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
  }

  private static String norm(String s) {
    return safe(s).toLowerCase(Locale.ROOT);
  }

  private static String nonBlank(String v, String def) {
    return safe(v).isBlank() ? def : safe(v);
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  @PreDestroy
  public void shutdownExecutors() {
    commentExecutor.shutdown();
  }

  private record TaskDraft(String title, String content, BigDecimal amount, String category) {}
}
