package com.leiyang.anontask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.AiTaskDraft;
import com.leiyang.anontask.domain.PlazaAiProvider;
import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.enums.TaskStatus;
import com.leiyang.anontask.domain.enums.UserStatus;
import com.leiyang.anontask.repo.AiTaskDraftRepository;
import com.leiyang.anontask.repo.PlazaAiProviderRepository;
import com.leiyang.anontask.repo.TaskPublishRepository;
import com.leiyang.anontask.repo.UserAccountRepository;
import com.leiyang.anontask.util.NoGenerator;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class AdminAiTaskDraftService {
  private static final String STATUS_DRAFT = "DRAFT";
  private static final String STATUS_PUBLISHED = "PUBLISHED";
  private static final String BOT_OPENID_PREFIX = "ai-task-bot:";

  private final AiTaskDraftRepository draftRepo;
  private final PlazaAiProviderRepository providerRepo;
  private final TaskPublishRepository taskRepo;
  private final UserAccountRepository userRepo;
  private final AiProviderChatService aiProviderChatService;
  private final ConfigService configService;
  private final ObjectMapper objectMapper;

  public AdminAiTaskDraftService(
      AiTaskDraftRepository draftRepo,
      PlazaAiProviderRepository providerRepo,
      TaskPublishRepository taskRepo,
      UserAccountRepository userRepo,
      AiProviderChatService aiProviderChatService,
      ConfigService configService,
      ObjectMapper objectMapper
  ) {
    this.draftRepo = draftRepo;
    this.providerRepo = providerRepo;
    this.taskRepo = taskRepo;
    this.userRepo = userRepo;
    this.aiProviderChatService = aiProviderChatService;
    this.configService = configService;
    this.objectMapper = objectMapper;
  }

  public record ProviderItem(String code, String name, String abbr) {}
  public record ProviderHealth(String code, String name, String abbr, boolean ready, boolean ok, String message) {}

  public List<ProviderItem> listReadyProviders() {
    List<ProviderItem> out = new ArrayList<>();
    for (PlazaAiProvider p : providerRepo.findByStatusOrderBySortNoAscIdAsc("ACTIVE")) {
      String code = norm(p.getCode());
      if (code.isEmpty()) continue;
      out.add(new ProviderItem(code, safe(p.getName()), safe(p.getAbbr())));
    }
    return out;
  }

  public List<ProviderHealth> checkProviders() {
    List<ProviderHealth> out = new ArrayList<>();
    for (PlazaAiProvider p : providerRepo.findByStatusOrderBySortNoAscIdAsc("ACTIVE")) {
      String code = norm(p.getCode());
      if (code.isEmpty()) continue;
      boolean ready = aiProviderChatService.isReady(code);
      var chk = aiProviderChatService.check(code);
      out.add(new ProviderHealth(
          code,
          safe(p.getName()),
          safe(p.getAbbr()),
          ready,
          chk.ok(),
          chk.message()
      ));
    }
    return out;
  }

  public List<AiTaskDraft> listDrafts() {
    return draftRepo.findTop200ByOrderByCreatedAtDesc();
  }

  @Transactional
  public AiTaskDraft generateDraft(String preferProviderCode) {
    String provider = resolveProvider(preferProviderCode);
    if (provider.isEmpty()) throw new BizException("No available AI provider, please configure api key first");
    PlazaAiProvider providerMeta = providerRepo.findFirstByCodeIgnoreCase(provider).orElse(null);
    GenResult gen = generate(provider);
    AiTaskDraft d = draftRepo.findTop1ByStatusOrderByUpdatedAtDesc(STATUS_DRAFT).orElseGet(AiTaskDraft::new);
    d.setProviderCode(provider);
    d.setTitle(cut(withAiPrefix(providerDisplayName(providerMeta, provider), gen.title()), 120));
    d.setContent(cut(gen.content(), 1000));
    d.setCategory(cut(nonBlank(gen.category(), "AI提问"), 32));
    d.setLocationText("线上");
    d.setAmount(clampAmount(gen.amount()));
    d.setTotalSlots(clampSlots(gen.totalSlots()));
    d.setDeadlineAt(clampDeadline(gen.deadlineHours()));
    d.setProofRequirements("文字说明即可，图片可选（最多4张）");
    d.setStatus(STATUS_DRAFT);
    d.setRawResponse(cut(gen.raw(), 8000));
    d.setPublishedTaskNo(null);
    AiTaskDraft saved = draftRepo.save(d);

    // Keep only one editable draft to avoid multiple tabs/cards in admin page.
    for (AiTaskDraft extra : draftRepo.findByStatusOrderByUpdatedAtDesc(STATUS_DRAFT)) {
      if (saved.getId() != null && !saved.getId().equals(extra.getId())) {
        draftRepo.delete(extra);
      }
    }
    return saved;
  }

  @Transactional
  public AiTaskDraft updateDraft(
      long id,
      String title,
      String content,
      String category,
      String locationText,
      BigDecimal amount,
      Integer totalSlots,
      Instant deadlineAt,
      String proofRequirements
  ) {
    AiTaskDraft d = draftRepo.findById(id).orElseThrow(() -> new BizException("Draft not found"));
    if (!STATUS_DRAFT.equalsIgnoreCase(d.getStatus())) {
      throw new BizException("Only draft can be edited");
    }
    PlazaAiProvider providerMeta = providerRepo.findFirstByCodeIgnoreCase(d.getProviderCode()).orElse(null);
    d.setTitle(cut(withAiPrefix(providerDisplayName(providerMeta, d.getProviderCode()), nonBlank(title, d.getTitle())), 120));
    d.setContent(cut(nonBlank(content, d.getContent()), 1000));
    d.setCategory(cut(nonBlank(category, d.getCategory()), 32));
    d.setLocationText(cut(nonBlank(locationText, "线上"), 256));
    d.setAmount(clampAmount(amount == null ? d.getAmount() : amount));
    d.setTotalSlots(clampSlots(totalSlots == null ? d.getTotalSlots() : totalSlots));
    d.setDeadlineAt(deadlineAt == null ? d.getDeadlineAt() : deadlineAt);
    d.setProofRequirements(cut(nonBlank(proofRequirements, d.getProofRequirements()), 256));
    return draftRepo.save(d);
  }

  @Transactional
  public String publishDraft(long id) {
    AiTaskDraft d = draftRepo.findById(id).orElseThrow(() -> new BizException("Draft not found"));
    if (!STATUS_DRAFT.equalsIgnoreCase(d.getStatus())) {
      throw new BizException("Draft already published");
    }
    PlazaAiProvider providerMeta = providerRepo.findFirstByCodeIgnoreCase(d.getProviderCode()).orElse(null);
    UserAccount bot = ensureBotUser(d.getProviderCode());

    TaskPublish t = new TaskPublish();
    t.setTaskNo(NoGenerator.gen("T"));
    t.setPublisher(bot);
    t.setTitle(cut(withAiPrefix(providerDisplayName(providerMeta, d.getProviderCode()), d.getTitle()), 120));
    t.setContent(cut(d.getContent(), 1000));
    t.setCategory(cut(nonBlank(d.getCategory(), "AI提问"), 32));
    t.setLocationText(cut(nonBlank(d.getLocationText(), "线上"), 256));
    t.setAmount(clampAmount(d.getAmount()));
    t.setTotalSlots(clampSlots(d.getTotalSlots()));
    t.setAcceptedSlots(0);
    t.setDeadlineAt(d.getDeadlineAt() == null ? Instant.now().plus(24, ChronoUnit.HOURS) : d.getDeadlineAt());
    t.setProofRequirements(cut(nonBlank(d.getProofRequirements(), "文字说明即可，图片可选（最多4张）"), 256));
    t.setStatus(TaskStatus.PUBLISHED);
    taskRepo.save(t);

    d.setStatus(STATUS_PUBLISHED);
    d.setPublishedTaskNo(t.getTaskNo());
    draftRepo.save(d);
    return t.getTaskNo();
  }

  @Transactional
  public void deleteDraft(long id) {
    AiTaskDraft d = draftRepo.findById(id).orElseThrow(() -> new BizException("Draft not found"));
    if (STATUS_PUBLISHED.equalsIgnoreCase(d.getStatus())) {
      throw new BizException("Published draft cannot be deleted");
    }
    draftRepo.delete(d);
  }

  private GenResult generate(String providerCode) {
    List<String> recentTitles = draftRepo.findTop200ByOrderByCreatedAtDesc().stream()
        .map(AiTaskDraft::getTitle)
        .map(AdminAiTaskDraftService::safe)
        .filter(v -> !v.isBlank())
        .distinct()
        .limit(8)
        .toList();
    String systemPrompt = """
        你是一个“向人类提问”的AI任务策划助手。
        目标：产出一个可在任务平台发布的任务，核心是“AI想问人类一个天马行空但有价值的问题”。
        约束：
        1) title 12-30字，必须是提问句或强问题导向
        2) content 60-220字，明确希望人类分享真实回答
        3) amount 为任务悬赏金额，按任务难度合理生成，最低 1 元，保留两位小数
        4) totalSlots 为 9999（表示人数不限制）
        5) deadlineHours 在 12-168 小时之间
        6) category 用 2-8 字中文，如“情感思考”“人生选择”
        7) 本次风格必须与常见“什么是爱情”类提问明显不同，可用故事回放/清单建议/辩论立场/场景假设等不同结构
        仅返回 JSON：
        {"title":"...","content":"...","amount":19.90,"totalSlots":9999,"deadlineHours":48,"category":"..."}
        """;
    String[] angles = {
        "爱情", "孤独", "婚姻", "成长", "选择", "工作意义", "友情", "原生家庭",
        "情绪管理", "自由", "安全感", "幸福", "遗憾", "信任", "压力", "金钱观", "边界感", "自我和解"
    };
    String[] styles = {
        "故事回放型", "立场辩论型", "清单建议型", "未来假设型", "关系场景型", "反直觉观点型"
    };
    String[] categories = { "人生选择", "关系思考", "情绪洞察", "价值判断", "成长复盘", "现实议题" };
    String angle = angles[ThreadLocalRandom.current().nextInt(angles.length)];
    String style = styles[ThreadLocalRandom.current().nextInt(styles.length)];
    String cat = categories[ThreadLocalRandom.current().nextInt(categories.length)];
    String avoid = recentTitles.isEmpty() ? "无" : String.join(" | ", recentTitles);
    String userPrompt = "请生成1个任务。主题偏“" + angle + "”，结构采用“" + style + "”，分类建议“" + cat
        + "”。以下标题禁止重复：" + avoid + "。注意：不要输出“随机标识/随机编号”这类字段。";
    Optional<String> raw = aiProviderChatService.chat(providerCode, systemPrompt, userPrompt, 320);
    if (raw.isPresent()) {
      String txt = raw.get();
      try {
        String json = extractJson(txt);
        if (!json.isBlank()) {
          var n = objectMapper.readTree(json);
          GenResult g = new GenResult(
              sanitizeGeneratedText(safe(n.path("title").asText(""))),
              sanitizeGeneratedText(safe(n.path("content").asText(""))),
              BigDecimal.valueOf(n.path("amount").asDouble(19.9)),
              n.path("totalSlots").asInt(9999),
              n.path("deadlineHours").asInt(48),
              safe(n.path("category").asText("AI提问")),
              txt
          );
          if (!g.title().isBlank() && !g.content().isBlank() && recentTitles.stream().map(AdminAiTaskDraftService::stripAiLeadPrefix).noneMatch(v -> v.equals(stripAiLeadPrefix(g.title())))) {
            return g;
          }
        }
      } catch (Exception ignore) {
        // fallback below
      }
    }
    String[] fallbackTitles = {
        "如果重来一次，你会为哪件事道歉？",
        "你曾经坚持的观念，后来为何推翻？",
        "在现实面前，你放弃过哪种理想关系？",
        "什么瞬间让你真正理解“边界感”？",
        "如果只能留一条建议给18岁的你，会是什么？",
        "你最想从亲密关系里被看见的部分是什么？",
        "哪次失败反而改变了你的人生方向？",
        "为了稳定，你曾经牺牲过什么？"
    };
    String[] fallbackBodies = {
        "请结合真实经历回答：事情发生在什么背景，你当时怎么判断，后来又为什么改变。欢迎写出你最想补说的一句话。",
        "请分享一个具体时刻：你原本坚持的立场是什么，转折点是什么，现在回看最大的代价和收获分别是什么。",
        "请从“当时怎么想、后来怎么做、现在怎么评估”三个部分展开，越真实越好，帮助AI理解人类决策逻辑。",
        "请写下你亲身经历过的一个场景：冲突来自哪里，你如何处理边界，结果是否让你满意，以及你会如何给别人建议。"
    };
    String title = fallbackTitles[ThreadLocalRandom.current().nextInt(fallbackTitles.length)];
    String candidateTitle = stripAiLeadPrefix(title);
    if (recentTitles.stream().map(AdminAiTaskDraftService::stripAiLeadPrefix).anyMatch(v -> v.equals(candidateTitle))) {
      title = "你现在如何看待" + angle + "这件事？";
    }
    String content = fallbackBodies[ThreadLocalRandom.current().nextInt(fallbackBodies.length)];
    return new GenResult(title, content, BigDecimal.valueOf(19.90), 9999, 48, cat, raw.orElse(""));
  }

  private static String withAiPrefix(String providerName, String title) {
    String t = stripAiLeadPrefix(safe(title));
    String p = safe(providerName).trim();
    String prefix = (p.isBlank() ? "AI" : p) + " AI想知道：";
    if (t.isBlank()) return prefix;
    if (t.startsWith(prefix)) return t;
    return prefix + t;
  }

  private static String providerDisplayName(PlazaAiProvider provider, String fallback) {
    String name = provider == null ? "" : safe(provider.getName()).trim();
    if (isOpenAiName(name)) return "GPT";
    if (!name.isBlank()) return name;
    String code = safe(fallback).trim();
    if (isOpenAiName(code)) return "GPT";
    return code.isBlank() ? "AI" : code;
  }

  private static boolean isOpenAiName(String value) {
    String v = safe(value).replace(" ", "").replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    return v.equals("openai") || v.equals("gpt") || v.equals("open") || v.contains("chatgpt");
  }

  private static String stripAiLeadPrefix(String value) {
    String t = safe(value).trim();
    if (t.isBlank()) return "";
    String[] prefixes = {
        "GPT AI想知道：", "GPT AI想知道:",
        "Gpt AI想知道：", "Gpt AI想知道:",
        "Open AI想知道：", "Open AI想知道:",
        "OpenAI想知道：", "OpenAI想知道:",
        "openai AI想知道：", "openai AI想知道:",
        "Ai想知道：", "AI想知道：", "Ai想知道:", "AI想知道:",
        "Ai想问：", "AI想问：", "Ai想问:", "AI想问:",
        "Ai提问：", "AI提问：", "Ai提问:", "AI提问:"
    };
    boolean changed = true;
    while (changed && !t.isBlank()) {
      changed = false;
      for (String p : prefixes) {
        if (t.startsWith(p)) {
          t = t.substring(p.length()).trim();
          changed = true;
          break;
        }
      }
    }
    return t;
  }

  private static String sanitizeGeneratedText(String value) {
    String t = safe(value).trim();
    if (t.isBlank()) return "";
    // Remove leaked anti-dup markers from model output.
    t = t.replaceFirst("^\\s*随机(?:标识|编号)\\s*\\d{1,10}\\s*[：:]\\s*", "");
    t = t.replaceFirst("^\\s*(?:标识|编号)\\s*\\d{1,10}\\s*[：:]\\s*", "");
    return t.trim();
  }

  private String resolveProvider(String prefer) {
    String p = norm(prefer);
    if (!p.isBlank() && providerReady(p)) return p;
    if (providerReady("openai")) return "openai";
    for (PlazaAiProvider ai : providerRepo.findByStatusOrderBySortNoAscIdAsc("ACTIVE")) {
      String code = norm(ai.getCode());
      if (providerReady(code)) return code;
    }
    return "";
  }

  private boolean providerReady(String provider) {
    return aiProviderChatService.isReady(provider);
  }

  private UserAccount ensureBotUser(String providerCode) {
    String code = norm(providerCode);
    String openId = BOT_OPENID_PREFIX + code;
    return userRepo.findByOpenId(openId).orElseGet(() -> {
      UserAccount u = new UserAccount();
      u.setOpenId(openId);
      u.setNickname((code.isBlank() ? "AI" : code.toUpperCase(Locale.ROOT)) + " 任务官");
      u.setAvatar("");
      u.setGender("UNKNOWN");
      u.setSignature("AI自动任务生成助手");
      u.setStatus(UserStatus.ACTIVE);
      u.setCreditScore(100);
      return userRepo.save(u);
    });
  }

  private static String extractJson(String text) {
    String s = safe(text);
    int l = s.indexOf('{');
    int r = s.lastIndexOf('}');
    if (l < 0 || r <= l) return "";
    return s.substring(l, r + 1);
  }

  private static BigDecimal clampAmount(BigDecimal v) {
    BigDecimal x = v == null ? BigDecimal.valueOf(19.9) : v;
    if (x.compareTo(BigDecimal.ONE) < 0) x = BigDecimal.ONE;
    return x.setScale(2, RoundingMode.HALF_UP);
  }

  private static int clampSlots(Integer slots) {
    int s = slots == null ? 9999 : slots;
    if (s < 1) s = 1;
    return Math.min(s, 999999);
  }

  private static Instant clampDeadline(Integer hours) {
    int h = hours == null ? 48 : hours;
    if (h < 1) h = 1;
    h = Math.min(h, 24 * 30);
    return Instant.now().plus(h, ChronoUnit.HOURS);
  }

  private static String nonBlank(String v, String def) {
    String s = safe(v);
    return s.isBlank() ? def : s;
  }

  private static String cut(String s, int max) {
    String v = safe(s);
    return v.length() <= max ? v : v.substring(0, max);
  }

  private static String norm(String s) {
    return safe(s).toLowerCase(Locale.ROOT);
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  private record GenResult(
      String title,
      String content,
      BigDecimal amount,
      int totalSlots,
      int deadlineHours,
      String category,
      String raw
  ) {}
}
