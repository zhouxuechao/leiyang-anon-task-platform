package com.leiyang.anontask.service;

import com.leiyang.anontask.domain.SysOpLog;
import com.leiyang.anontask.repo.SysOpLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OpLogService {
  private final SysOpLogRepository repo;

  public OpLogService(SysOpLogRepository repo) {
    this.repo = repo;
  }

  public void log(String actorType, long actorId, String method, String path, String ip, String ua, boolean ok, String err) {
    SysOpLog l = new SysOpLog();
    l.setActorType(actorType);
    l.setActorId(actorId);
    l.setMethod(method);
    l.setPath(path);
    l.setIp(ip);
    l.setUserAgent(ua == null ? null : trim(ua, 250));
    l.setOkFlag(ok);
    l.setErrorMsg(err == null ? null : trim(err, 250));
    l.setCreatedAt(Instant.now());
    repo.save(l);
  }

  private static String trim(String s, int max) {
    if (s == null) return null;
    if (s.length() <= max) return s;
    return s.substring(0, max);
  }
}

