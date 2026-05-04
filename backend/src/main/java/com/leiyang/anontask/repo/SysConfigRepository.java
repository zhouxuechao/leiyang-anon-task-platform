package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.SysConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysConfigRepository extends JpaRepository<SysConfig, Long> {
  Optional<SysConfig> findByCfgKey(String cfgKey);
}

