package com.leiyang.anontask.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leiyang.anontask.domain.PlazaCategory;
import com.leiyang.anontask.repo.PlazaAiProviderRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiCategoryClassifierService {
  private final PlazaAiProviderRepository aiProviderRepo;
  private final AiProviderChatService aiProviderChatService;
  private final ObjectMapper objectMapper;

  public AiCategoryClassifierService(
      PlazaAiProviderRepository aiProviderRepo,
      AiProviderChatService aiProviderChatService,
      ObjectMapper objectMapper
  ) {
    this.aiProviderRepo = aiProviderRepo;
    this.aiProviderChatService = aiProviderChatService;
    this.objectMapper = objectMapper;
  }

  public record Classification(String matchedCode, String suggestedName, double confidence) {}

  public Optional<Classification> classify(String content, List<PlazaCategory> activeCategories) {
    String text = trim(content);
    if (text.isEmpty() || activeCategories == null || activeCategories.isEmpty()) return Optional.empty();
    String provider = resolveProvider();
    if (provider.isEmpty()) return Optional.empty();

    String categories = activeCategories.stream()
        .map(v -> v.getCode() + " | " + trim(v.getName()) + " | " + trim(v.getKeywords()))
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");

    String systemPrompt = """
        你是一个内容分类器。只从给定分类中选择最匹配的一项。
        若无法匹配，categoryCode 输出 OTHER，并给 suggestedName（2-8字中文，简短名词）。
        仅返回 JSON，不要返回其它文字。
        JSON 格式: {"categoryCode":"CODE","suggestedName":"可空","confidence":0.0}
        """;
    String userPrompt = "分类列表:\n" + categories + "\n\n待分类内容:\n" + text;
    var attempt = aiProviderChatService.chatOnce(provider, systemPrompt, userPrompt, 120);
    if (!attempt.ok()) return Optional.empty();
    return parseContent(attempt.content(), activeCategories);
  }

  private String resolveProvider() {
    for (var p : aiProviderRepo.findByStatusOrderBySortNoAscIdAsc("ACTIVE")) {
      String code = trim(p.getCode()).toLowerCase(Locale.ROOT);
      if (aiProviderChatService.isReady(code)) return code;
    }
    return "";
  }

  private Optional<Classification> parseContent(String content, List<PlazaCategory> activeCategories) {
    try {
      String json = extractJson(content);
      if (json.isEmpty()) return Optional.empty();
      JsonNode r = objectMapper.readTree(json);
      String categoryCode = normalizeCode(r.path("categoryCode").asText(""));
      String suggestedName = trim(r.path("suggestedName").asText(""));
      double confidence = r.path("confidence").asDouble(0.0);

      if (categoryCode.isEmpty()) return Optional.empty();
      String matched = matchCodeOrName(categoryCode, activeCategories).orElse("");
      if (!matched.isEmpty()) return Optional.of(new Classification(matched, "", confidence));
      if ("OTHER".equals(categoryCode)) return Optional.of(new Classification("OTHER", suggestedName, confidence));
      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private static String extractJson(String text) {
    String v = trim(text);
    int start = v.indexOf('{');
    int end = v.lastIndexOf('}');
    if (start < 0 || end <= start) return "";
    return v.substring(start, end + 1);
  }

  private static Optional<String> matchCodeOrName(String candidate, List<PlazaCategory> categories) {
    String code = normalizeCode(candidate);
    for (PlazaCategory c : categories) {
      if (normalizeCode(c.getCode()).equals(code)) return Optional.of(normalizeCode(c.getCode()));
      if (trim(c.getName()).equalsIgnoreCase(trim(candidate))) return Optional.of(normalizeCode(c.getCode()));
    }
    return Optional.empty();
  }

  private static String normalizeCode(String s) {
    return trim(s).toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
