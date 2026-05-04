package com.leiyang.anontask.service;

import com.leiyang.anontask.domain.SysSensitiveWord;
import com.leiyang.anontask.domain.enums.GeneralStatus;
import com.leiyang.anontask.domain.enums.SensitiveWordAction;
import com.leiyang.anontask.repo.SysSensitiveWordRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SensitiveWordService {
  private final SysSensitiveWordRepository repo;

  public SensitiveWordService(SysSensitiveWordRepository repo) {
    this.repo = repo;
  }

  public SensitiveWordAction evaluate(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    List<SysSensitiveWord> words = repo.findByStatus(GeneralStatus.ACTIVE);
    SysSensitiveWord hit = words.stream()
        .filter(w -> text.contains(w.getWord()))
        .max(Comparator.comparingInt(SysSensitiveWord::getLevel))
        .orElse(null);
    return hit == null ? null : hit.getActionType();
  }
}

