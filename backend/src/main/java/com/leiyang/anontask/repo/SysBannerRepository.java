package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.SysBanner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysBannerRepository extends JpaRepository<SysBanner, Long> {
  List<SysBanner> findTop10ByStatusOrderBySortNoAscCreatedAtDesc(String status);
}

