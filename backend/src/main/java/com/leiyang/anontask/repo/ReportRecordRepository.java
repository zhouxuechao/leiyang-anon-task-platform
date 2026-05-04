package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.ReportRecord;
import com.leiyang.anontask.domain.enums.ReportStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRecordRepository extends JpaRepository<ReportRecord, Long> {
  Optional<ReportRecord> findByReportNo(String reportNo);
  List<ReportRecord> findTop50ByStatusOrderByCreatedAtDesc(ReportStatus status);
  Page<ReportRecord> findByStatus(ReportStatus status, Pageable pageable);
}
