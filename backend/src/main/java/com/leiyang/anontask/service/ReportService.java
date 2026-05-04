package com.leiyang.anontask.service;

import com.leiyang.anontask.domain.ReportRecord;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.enums.ReportStatus;
import com.leiyang.anontask.repo.ReportRecordRepository;
import com.leiyang.anontask.util.NoGenerator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
  private final ReportRecordRepository repo;

  public ReportService(ReportRecordRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public String create(UserAccount reporter, String targetType, long targetId, String reason) {
    ReportRecord r = new ReportRecord();
    r.setReportNo(NoGenerator.gen("R"));
    r.setReporter(reporter);
    r.setTargetType(targetType);
    r.setTargetId(targetId);
    r.setReason(reason);
    r.setStatus(ReportStatus.PENDING);
    repo.save(r);
    return r.getReportNo();
  }
}

