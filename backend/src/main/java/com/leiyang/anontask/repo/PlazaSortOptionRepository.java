package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaSortOption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlazaSortOptionRepository extends JpaRepository<PlazaSortOption, Long> {
  Optional<PlazaSortOption> findByCode(String code);

  List<PlazaSortOption> findByStatusOrderBySortNoAscIdAsc(String status);
}
