package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.SysSensitiveWord;
import com.leiyang.anontask.domain.enums.GeneralStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysSensitiveWordRepository extends JpaRepository<SysSensitiveWord, Long> {
  List<SysSensitiveWord> findByStatus(GeneralStatus status);
}

