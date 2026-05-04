package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlazaCategoryRepository extends JpaRepository<PlazaCategory, Long> {
  Optional<PlazaCategory> findByCode(String code);

  Optional<PlazaCategory> findByName(String name);

  List<PlazaCategory> findByStatusOrderBySortNoAscIdAsc(String status);
}
