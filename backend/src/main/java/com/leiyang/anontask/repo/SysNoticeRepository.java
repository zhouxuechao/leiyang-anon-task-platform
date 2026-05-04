package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.SysNotice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysNoticeRepository extends JpaRepository<SysNotice, Long> {
  List<SysNotice> findTop20ByStatusOrderBySortNoAscCreatedAtDesc(String status);
}

