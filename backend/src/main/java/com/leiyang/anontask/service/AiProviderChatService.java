package com.leiyang.anontask.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leiyang.anontask.domain.PlazaAiProvider;
import com.leiyang.anontask.repo.PlazaAiProviderRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiProviderChatService {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);

  private final PlazaAiProviderRepository providerRepo;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public AiProviderChatService(PlazaAiProviderRepository providerRepo, ObjectMapper objectMapper) {
    this.providerRepo = providerRepo;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  }

  public record ProviderCheck(boolean ready, boolean ok, String message, String providerCode) {}

  public boolean isReady(String providerCode) {
    return resolve(providerCode).map(Resolved::ready).orElse(false);
  }

  public ProviderCheck check(String providerCode) {
    String code = trim(providerCode).toLowerCase(Locale.ROOT);
    if (code.isEmpty()) return new ProviderCheck(false, false, "provider code is empty", code);
    var rOpt = resolve(code);
    if (rOpt.isEmpty() || !rOpt.get().ready()) {
      return new ProviderCheck(false, false, "provider not configured or disabled", code);
    }
    Resolved r = rOpt.get();
    long start = System.currentTimeMillis();
    ChatAttempt attempt = chatOnce(r.providerCode(), "你是连通性检测助手，只回答OK。", "只输出OK（两个字母）", 120);
    long cost = Math.max(1, System.currentTimeMillis() - start);
    if (attempt.ok()) return new ProviderCheck(true, true, "ok (" + cost + "ms)", r.providerCode());
    return new ProviderCheck(true, false, attempt.error(), r.providerCode());
  }

  public record ChatAttempt(boolean ok, String content, String error) {}

  public ChatAttempt chatOnce(String providerCode, String systemPrompt, String userPrompt, int maxTokens) {
    var rOpt = resolve(providerCode);
    if (rOpt.isEmpty() || !rOpt.get().ready()) {
      return new ChatAttempt(false, "", "provider not configured or disabled");
    }
    Resolved r = rOpt.get();
    try {
      ChatResponse resp = requestChat(r, systemPrompt, userPrompt, maxTokens, r.temperature());
      if (!resp.ok()) {
        return new ChatAttempt(false, "", "HTTP " + resp.statusCode() + " " + safeErr(resp.error()));
      }
      String content = trim(resp.content());
      if (content.isEmpty()) return new ChatAttempt(false, "", "empty response");
      return new ChatAttempt(true, content, "");
    } catch (IOException e) {
      return new ChatAttempt(false, "", safeErr(e.getMessage()));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ChatAttempt(false, "", "interrupted");
    } catch (RuntimeException e) {
      return new ChatAttempt(false, "", safeErr(e.getMessage()));
    }
  }

  public Optional<String> chat(String providerCode, String systemPrompt, String userPrompt, int maxTokens) {
    var rOpt = resolve(providerCode);
    if (rOpt.isEmpty() || !rOpt.get().ready()) return Optional.empty();
    Resolved r = rOpt.get();
    try {
      ChatResponse resp = requestChat(r, systemPrompt, userPrompt, maxTokens, r.temperature());
      if (resp.ok()) {
        String content = trim(resp.content());
        if (!content.isEmpty()) return Optional.of(content);
      }
      // Some providers may return empty content under low token budget.
      int retryTokens = Math.max(maxTokens + 120, maxTokens * 2);
      ChatResponse retry = requestChat(
          r,
          systemPrompt,
          userPrompt + "\n请只输出最终答案，不要输出思考过程。",
          retryTokens,
          r.temperature()
      );
      if (!retry.ok()) return Optional.empty();
      String retryContent = trim(retry.content());
      if (retryContent.isEmpty()) return Optional.empty();
      return Optional.of(retryContent);
    } catch (IOException | InterruptedException e) {
      return Optional.empty();
    }
  }

  public Optional<String> chatWithImages(
      String providerCode,
      String systemPrompt,
      String userPrompt,
      List<String> imageUrls,
      int maxTokens
  ) {
    var rOpt = resolve(providerCode);
    if (rOpt.isEmpty() || !rOpt.get().ready()) return Optional.empty();
    Resolved r = rOpt.get();
    List<String> urls = imageUrls == null ? List.of() : imageUrls.stream().map(AiProviderChatService::trim).filter(v -> !v.isEmpty()).toList();
    if (urls.isEmpty()) {
      return chat(providerCode, systemPrompt, userPrompt, maxTokens);
    }
    try {
      List<Map<String, Object>> userContent = new ArrayList<>();
      userContent.add(Map.of("type", "text", "text", trim(userPrompt)));
      for (String url : urls) {
        userContent.add(Map.of(
            "type", "image_url",
            "image_url", Map.of("url", url)
        ));
      }
      ChatResponse resp = requestChat(r, systemPrompt, userContent, maxTokens, r.temperature());
      if (!resp.ok()) {
        String fallbackPrompt = userPrompt + "\n图片信息：" + String.join("；", urls);
        return chat(providerCode, systemPrompt, fallbackPrompt, maxTokens);
      }
      String content = trim(resp.content());
      if (content.isEmpty()) {
        String fallbackPrompt = userPrompt + "\n图片信息：" + String.join("；", urls);
        return chat(providerCode, systemPrompt, fallbackPrompt, Math.max(maxTokens + 120, maxTokens * 2));
      }
      return Optional.of(content);
    } catch (IOException | InterruptedException e) {
      String fallbackPrompt = userPrompt + "\n图片信息：" + String.join("；", urls);
      return chat(providerCode, systemPrompt, fallbackPrompt, maxTokens);
    }
  }

  private String defaultBaseUrl(String provider) {
    return switch (trim(provider).toLowerCase(Locale.ROOT)) {
      case "openai" -> "https://api.openai.com/v1";
      case "gpt" -> "https://api.openai.com/v1";
      case "deepseek" -> "https://api.deepseek.com/v1";
      case "qwen", "dashscope" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
      case "glm" -> "https://open.bigmodel.cn/api/paas/v4";
      case "doubao", "ark" -> "https://ark.cn-beijing.volces.com/api/v3";
      case "kimi", "moonshot" -> "https://api.moonshot.cn/v1";
      default -> "";
    };
  }

  private static String tokenFieldName(String model) {
    String m = trim(model).toLowerCase(Locale.ROOT);
    if (m.startsWith("gpt-5")) return "max_completion_tokens";
    return "max_tokens";
  }

  private static String stripTrailingSlash(String s) {
    String v = trim(s);
    while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
    return v;
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private Resolved resolveStrict(String provider) {
    String code = trim(provider).toLowerCase(Locale.ROOT);
    PlazaAiProvider row = providerRepo.findFirstByCodeIgnoreCase(code).orElse(null);
    if (row == null) return new Resolved(code, false, "", "", "", 6000, 0.7, false);
    String model = trim(row.getModel());
    String apiKey = sanitizeApiKey(row.getApiKey());
    String baseUrl = normalizeBaseUrl(row.getBaseUrl(), code);
    if (baseUrl.isEmpty()) baseUrl = normalizeBaseUrl(defaultBaseUrl(code), code);
    int timeoutMs = normalizeTimeoutMs(code, row.getTimeoutMs() == null ? 6000 : row.getTimeoutMs());
    double temperature = row.getTemperature() == null ? 0.7 : row.getTemperature();
    boolean enabled = "ACTIVE".equalsIgnoreCase(trim(row.getStatus()));
    boolean ready = enabled && !model.isEmpty() && !apiKey.isEmpty() && !baseUrl.isEmpty();
    return new Resolved(code, enabled, model, apiKey, baseUrl, timeoutMs, temperature, ready);
  }

  private Optional<Resolved> resolve(String provider) {
    String raw = trim(provider).toLowerCase(Locale.ROOT);
    if (raw.isEmpty()) return Optional.empty();
    for (String c : aliases(raw)) {
      Resolved r = resolveStrict(c);
      if (r.ready()) return Optional.of(r);
    }
    return Optional.of(resolveStrict(raw));
  }

  private static List<String> aliases(String provider) {
    String p = trim(provider).toLowerCase(Locale.ROOT);
    List<String> out = new ArrayList<>();
    if (p.isEmpty()) return out;
    out.add(p);
    switch (p) {
      case "gpt" -> out.add("openai");
      case "openai" -> out.add("gpt");
      case "doubao" -> out.add("ark");
      case "ark" -> out.add("doubao");
      case "qwen" -> out.add("dashscope");
      case "dashscope" -> out.add("qwen");
      case "kimi" -> out.add("moonshot");
      case "moonshot" -> out.add("kimi");
      default -> {}
    }
    return out;
  }

  private ChatResponse requestChat(
      Resolved r,
      String systemPrompt,
      String userPrompt,
      int maxTokens,
      double temperature
  ) throws IOException, InterruptedException {
    return requestChat(r, systemPrompt, (Object) trim(userPrompt), maxTokens, temperature);
  }

  private ChatResponse requestChat(
      Resolved r,
      String systemPrompt,
      Object userContent,
      int maxTokens,
      double temperature
  ) throws IOException, InterruptedException {
    Map<String, Object> payload = new HashMap<>();
    payload.put("model", r.model());
    payload.put("temperature", normalizeTemperature(r.providerCode(), r.model(), temperature));
    payload.put(tokenFieldName(r.model()), Math.max(32, maxTokens));
    if (isQwenProvider(r.providerCode(), r.model())) {
      payload.put("enable_thinking", false);
    }
    payload.put("messages", List.of(
        Map.of("role", "system", "content", trim(systemPrompt)),
        Map.of("role", "user", "content", normalizeUserContent(r.providerCode(), r.model(), userContent))
    ));

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(chatCompletionsUrl(r.baseUrl())))
        .timeout(Duration.ofMillis(Math.max(2000, r.timeoutMs())))
        .header("Authorization", "Bearer " + r.apiKey())
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
        .build();
    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      return new ChatResponse(false, "", resp.statusCode(), trim(resp.body()));
    }
    JsonNode root = objectMapper.readTree(resp.body());
    JsonNode choices = root.path("choices");
    if (!choices.isArray() || choices.isEmpty()) {
      return new ChatResponse(false, "", resp.statusCode(), "choices empty");
    }
    JsonNode message = choices.get(0).path("message");
    String content = extractContent(message.path("content"));
    if (content.isEmpty()) {
      // Some providers occasionally put usable text in reasoning_content while content is empty.
      content = extractContent(message.path("reasoning_content"));
    }
    return new ChatResponse(true, content, resp.statusCode(), "");
  }

  private static String chatCompletionsUrl(String baseUrl) {
    String b = trim(baseUrl);
    if (b.endsWith("/chat/completions")) return b;
    return stripTrailingSlash(b) + "/chat/completions";
  }

  private static String normalizeBaseUrl(String raw, String provider) {
    String v = trim(raw);
    if (v.isEmpty()) return "";
    int i = v.indexOf("http://");
    int j = v.indexOf("https://");
    int start = -1;
    if (i >= 0 && j >= 0) start = Math.min(i, j);
    else start = Math.max(i, j);
    if (start >= 0 && start > 0) v = v.substring(start);
    int space = v.indexOf(' ');
    if (space > 0) v = v.substring(0, space);
    v = stripTrailingSlash(v);
    if (v.endsWith("/chat/completions")) {
      if ("glm".equals(provider)) {
        return v.substring(0, v.length() - "/chat/completions".length());
      }
      if ("doubao".equals(provider) || "ark".equals(provider) || "openai".equals(provider) || "gpt".equals(provider)
          || "qwen".equals(provider) || "dashscope".equals(provider) || "deepseek".equals(provider)
          || "kimi".equals(provider) || "moonshot".equals(provider)) {
        return v.substring(0, v.length() - "/chat/completions".length());
      }
    }
    return v;
  }

  private static String safeErr(String s) {
    String v = trim(s);
    if (v.isEmpty()) return "unknown error";
    return v.length() > 120 ? v.substring(0, 120) : v;
  }

  private static String extractContent(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) return "";
    if (node.isTextual()) return trim(node.asText(""));
    if (node.isArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonNode item : node) {
        if (item == null || item.isNull() || item.isMissingNode()) continue;
        if (item.isTextual()) {
          String t = trim(item.asText(""));
          if (!t.isEmpty()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(t);
          }
          continue;
        }
        if (item.isObject()) {
          String t = trim(item.path("text").asText(""));
          if (t.isEmpty()) t = trim(item.path("content").asText(""));
          if (!t.isEmpty()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(t);
          }
        }
      }
      return trim(sb.toString());
    }
    if (node.isObject()) {
      String t = trim(node.path("text").asText(""));
      if (t.isEmpty()) t = trim(node.path("content").asText(""));
      return t;
    }
    return trim(node.asText(""));
  }

  private static String sanitizeApiKey(String raw) {
    String v = trim(raw);
    if (v.isEmpty()) return "";
    if (v.startsWith("\"") && v.endsWith("\"") && v.length() > 1) v = v.substring(1, v.length() - 1).trim();
    if (v.toLowerCase(Locale.ROOT).startsWith("bearer ")) v = trim(v.substring("bearer ".length()));
    return v;
  }

  private static double normalizeTemperature(String provider, String model, double raw) {
    String p = trim(provider).toLowerCase(Locale.ROOT);
    String m = trim(model).toLowerCase(Locale.ROOT);
    if ("kimi".equals(p) || "moonshot".equals(p) || m.startsWith("kimi-k2")) {
      return 1.0;
    }
    return raw;
  }

  private static Object normalizeUserContent(String provider, String model, Object userContent) {
    if (!isQwenProvider(provider, model) || !(userContent instanceof String s)) return userContent;
    String text = trim(s);
    if (text.contains("/no_think")) return text;
    return text + "\n/no_think";
  }

  private static boolean isQwenProvider(String provider, String model) {
    String p = trim(provider).toLowerCase(Locale.ROOT);
    String m = trim(model).toLowerCase(Locale.ROOT);
    return "qwen".equals(p) || "dashscope".equals(p) || m.startsWith("qwen");
  }

  private static int normalizeTimeoutMs(String provider, int raw) {
    int v = Math.max(2000, raw);
    return Math.max(v, 20000);
  }

  private record Resolved(
      String providerCode,
      boolean enabled,
      String model,
      String apiKey,
      String baseUrl,
      int timeoutMs,
      double temperature,
      boolean ready
  ) {}

  private record ChatResponse(boolean ok, String content, int statusCode, String error) {}
}
