package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.repo.UserMessageRepository;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.MessageService;
import com.leiyang.anontask.service.UserService;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mp/messages")
public class MpMessageController {
  private final UserService userService;
  private final UserMessageRepository repo;
  private final MessageService messageService;

  public MpMessageController(UserService userService, UserMessageRepository repo, MessageService messageService) {
    this.userService = userService;
    this.repo = repo;
    this.messageService = messageService;
  }

  public record MessageItem(long id, String type, String title, String content, boolean read, String createdAt) {}
  public record UnreadResp(long count) {}

  @GetMapping("")
  public ApiResult<List<MessageItem>> list() {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    var items = repo.findTop50ByUserOrderByCreatedAtDesc(user).stream()
        .map(m -> new MessageItem(
            m.getId(),
            m.getMsgType(),
            m.getTitle(),
            m.getContent(),
            m.isReadFlag(),
            m.getCreatedAt().toString()
        ))
        .toList();
    return ApiResult.ok(items);
  }

  @GetMapping("/unread-count")
  public ApiResult<UnreadResp> unreadCount() {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    return ApiResult.ok(new UnreadResp(messageService.unreadCount(user)));
  }

  @PostMapping("/read-all")
  @Transactional
  public ApiResult<Void> readAll() {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    for (var m : repo.findByUserAndReadFlagFalse(user)) {
      m.setReadFlag(true);
      repo.save(m);
    }
    messageService.clearUnread(user);
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/read")
  @Transactional
  public ApiResult<Void> read(@PathVariable long id) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    var m = repo.findById(id).orElse(null);
    if (m != null && m.getUser().getId().equals(user.getId()) && !m.isReadFlag()) {
      m.setReadFlag(true);
      repo.save(m);
      messageService.markOneRead(user);
    }
    return ApiResult.ok(null);
  }
}
