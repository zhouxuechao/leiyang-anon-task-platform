package com.leiyang.anontask.service;

import com.leiyang.anontask.config.AsyncMessagingConfig;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.UserMessage;
import com.leiyang.anontask.repo.UserAccountRepository;
import com.leiyang.anontask.repo.UserMessageRepository;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
  private static final Logger log = LoggerFactory.getLogger(MessageService.class);
  private static final String UNREAD_KEY_PREFIX = "msg:unread:";

  private final UserMessageRepository repo;
  private final UserAccountRepository userRepo;
  private final ObjectProvider<StringRedisTemplate> redisProvider;
  private final MessageQueueService queueService;

  public MessageService(
      UserMessageRepository repo,
      UserAccountRepository userRepo,
      ObjectProvider<StringRedisTemplate> redisProvider,
      MessageQueueService queueService
  ) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.redisProvider = redisProvider;
    this.queueService = queueService;
  }

  public record UserMessageEvent(Long userId, String type, String title, String content) {}

  public void send(UserAccount user, String type, String title, String content) {
    if (user == null || user.getId() == null) return;
    if (queueService.publish(AsyncMessagingConfig.MESSAGE_ROUTING_KEY, new UserMessageEvent(user.getId(), type, title, content))) {
      log.info("message_queued userId={} type={}", user.getId(), type);
      return;
    }
    log.info("message_direct userId={} type={}", user.getId(), type);
    saveDirect(user.getId(), type, title, content);
  }

  public void saveDirect(Long userId, String type, String title, String content) {
    if (userId == null) return;
    UserAccount user = userRepo.findById(userId).orElse(null);
    if (user == null) {
      log.warn("message_save_skipped userId={} reason=userNotFound type={}", userId, type);
      return;
    }
    UserMessage m = new UserMessage();
    m.setUser(user);
    m.setMsgType(type);
    m.setTitle(title);
    m.setContent(content);
    m.setReadFlag(false);
    m.setCreatedAt(Instant.now());
    repo.save(m);
    incrementUnread(userId);
    log.info("message_saved userId={} messageId={} type={}", userId, m.getId(), type);
  }

  public long unreadCount(UserAccount user) {
    if (user == null || user.getId() == null) return 0;
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) {
        String key = unreadKey(user.getId());
        String cached = redis.opsForValue().get(key);
        if (cached != null) return Long.parseLong(cached);
        long count = repo.countByUserAndReadFlagFalse(user);
        redis.opsForValue().set(key, String.valueOf(count), 7, TimeUnit.DAYS);
        return count;
      }
    } catch (Exception ignored) {
      // fall back to database
    }
    return repo.countByUserAndReadFlagFalse(user);
  }

  public void markOneRead(UserAccount user) {
    if (user == null || user.getId() == null) return;
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) {
        Long value = redis.opsForValue().decrement(unreadKey(user.getId()));
        if (value != null && value < 0) redis.opsForValue().set(unreadKey(user.getId()), "0", 7, TimeUnit.DAYS);
      }
    } catch (Exception ignored) {
      // cache miss is harmless
    }
  }

  public void clearUnread(UserAccount user) {
    if (user == null || user.getId() == null) return;
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) redis.opsForValue().set(unreadKey(user.getId()), "0", 7, TimeUnit.DAYS);
    } catch (Exception ignored) {
      // cache miss is harmless
    }
  }

  private void incrementUnread(Long userId) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) {
        redis.opsForValue().increment(unreadKey(userId));
        redis.expire(unreadKey(userId), 7, TimeUnit.DAYS);
      }
    } catch (Exception ignored) {
      // unread count can be rebuilt from database
    }
  }

  private static String unreadKey(Long userId) {
    return UNREAD_KEY_PREFIX + userId;
  }
}
