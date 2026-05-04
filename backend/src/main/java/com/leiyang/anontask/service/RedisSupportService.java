package com.leiyang.anontask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisSupportService {
  private static final Logger log = LoggerFactory.getLogger(RedisSupportService.class);
  private static final ConcurrentHashMap<String, Instant> LOCAL_LOCKS = new ConcurrentHashMap<>();

  private final ObjectProvider<StringRedisTemplate> redisProvider;
  private final ObjectMapper mapper;

  public RedisSupportService(ObjectProvider<StringRedisTemplate> redisProvider, ObjectMapper mapper) {
    this.redisProvider = redisProvider;
    this.mapper = mapper;
  }

  public boolean setIfAbsent(String key, String value, Duration ttl) {
    long ttlSeconds = Math.max(1, ttl.toSeconds());
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) {
        log.warn("redis_lock_local_fallback key={} reason=templateMissing", key);
        return setLocalIfAbsent(key, ttlSeconds);
      }
      Boolean ok = redis.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
      return Boolean.TRUE.equals(ok);
    } catch (Exception e) {
      log.warn("redis_lock_local_fallback key={} error={}", key, e.toString());
      return setLocalIfAbsent(key, ttlSeconds);
    }
  }

  private boolean setLocalIfAbsent(String key, long ttlSeconds) {
    Instant now = Instant.now();
    LOCAL_LOCKS.entrySet().removeIf(e -> e.getValue().isBefore(now));
    return LOCAL_LOCKS.putIfAbsent(key, now.plusSeconds(ttlSeconds)) == null;
  }

  public <T> T getJson(String key, Class<T> type) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) return null;
      String raw = redis.opsForValue().get(key);
      if (raw == null || raw.isBlank()) return null;
      return mapper.readValue(raw, type);
    } catch (Exception e) {
      return null;
    }
  }

  public void setJson(String key, Object value, Duration ttl) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null || value == null) return;
      redis.opsForValue().set(key, mapper.writeValueAsString(value), Math.max(1, ttl.toSeconds()), TimeUnit.SECONDS);
    } catch (Exception ignored) {
      // Cache misses fall back to database.
    }
  }

  public void delete(String key) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) redis.delete(key);
    } catch (Exception ignored) {
      // no-op
    }
  }
}
