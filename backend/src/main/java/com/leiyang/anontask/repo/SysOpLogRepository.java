package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.SysOpLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysOpLogRepository extends JpaRepository<SysOpLog, Long> {
  List<SysOpLog> findTop200ByOrderByCreatedAtDesc();
}
