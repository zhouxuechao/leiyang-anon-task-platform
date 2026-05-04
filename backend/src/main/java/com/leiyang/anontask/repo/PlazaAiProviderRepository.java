package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaAiProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlazaAiProviderRepository extends JpaRepository<PlazaAiProvider, Long> {
  Optional<PlazaAiProvider> findByCode(String code);
  Optional<PlazaAiProvider> findFirstByCodeIgnoreCase(String code);

  List<PlazaAiProvider> findByStatusOrderBySortNoAscIdAsc(String status);
}
