package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.enums.UserStatus;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.UserAccountRepository;
import com.leiyang.anontask.repo.WalletAccountRepository;
import com.leiyang.anontask.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
  private final UserAccountRepository userRepo;
  private final WalletAccountRepository walletRepo;
  private final WalletService walletService;

  public AdminUserController(UserAccountRepository userRepo, WalletAccountRepository walletRepo, WalletService walletService) {
    this.userRepo = userRepo;
    this.walletRepo = walletRepo;
    this.walletService = walletService;
  }

  public record UserItem(
      long id,
      String openId,
      String nickname,
      String status,
      int creditScore,
      String gender,
      String balance,
      String frozen,
      String createdAt
  ) {}

  public record CreditReq(@NotNull(message = "creditScore is required") @Min(0) Integer creditScore) {}
  public record AdjustBalanceReq(@NotNull(message = "amount is required") BigDecimal amount) {}

  @GetMapping("")
  public ApiResult<PageResponse<UserItem>> list(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String gender,
      @RequestParam(required = false) Integer minCredit,
      @RequestParam(required = false) Integer maxCredit,
      @RequestParam(required = false) String balanceState,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    String query = safe(q).toLowerCase(Locale.ROOT);
    String st = safe(status).toUpperCase(Locale.ROOT);
    String gd = safe(gender).toUpperCase(Locale.ROOT);
    String bs = safe(balanceState).toUpperCase(Locale.ROOT);
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    var rows = userRepo.findAll().stream()
        .filter(u -> id == null || u.getId().equals(id))
        .filter(u -> query.isEmpty()
            || String.valueOf(u.getId()).contains(query)
            || safe(u.getOpenId()).toLowerCase(Locale.ROOT).contains(query)
            || safe(u.getNickname()).toLowerCase(Locale.ROOT).contains(query)
            || safe(u.getSignature()).toLowerCase(Locale.ROOT).contains(query))
        .filter(u -> st.isEmpty() || u.getStatus().name().equals(st))
        .filter(u -> gd.isEmpty() || safe(u.getGender()).toUpperCase(Locale.ROOT).equals(gd))
        .filter(u -> minCredit == null || u.getCreditScore() >= minCredit)
        .filter(u -> maxCredit == null || u.getCreditScore() <= maxCredit)
        .map(u -> {
          var w = walletRepo.findByUser(u).orElse(null);
          BigDecimal balance = w == null ? BigDecimal.ZERO : w.getBalance();
          BigDecimal frozen = w == null ? BigDecimal.ZERO : w.getFrozenAmount();
          return new UserRow(u, balance, frozen);
        })
        .filter(r -> switch (bs) {
          case "HAS_BALANCE" -> r.balance().compareTo(BigDecimal.ZERO) > 0;
          case "FROZEN" -> r.frozen().compareTo(BigDecimal.ZERO) > 0;
          case "ZERO" -> r.balance().compareTo(BigDecimal.ZERO) == 0 && r.frozen().compareTo(BigDecimal.ZERO) == 0;
          default -> true;
        })
        .sorted(Comparator.comparing((UserRow r) -> r.user().getId()).reversed())
        .toList();
    long total = rows.size();
    var list = rows.stream()
        .skip((long) (safePage - 1) * safeSize)
        .limit(safeSize)
        .map(r -> new UserItem(
            r.user().getId(),
            r.user().getOpenId(),
            r.user().getNickname(),
            r.user().getStatus().name(),
            r.user().getCreditScore(),
            safe(r.user().getGender()),
            r.balance().toPlainString(),
            r.frozen().toPlainString(),
            r.user().getCreatedAt().toString()
        ))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, total, list));
  }

  private record UserRow(UserAccount user, BigDecimal balance, BigDecimal frozen) {}

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  @PostMapping("/{id}/ban")
  @Transactional
  public ApiResult<Void> ban(@PathVariable long id) {
    UserAccount u = userRepo.findById(id).orElseThrow(() -> new BizException("User not found"));
    u.setStatus(UserStatus.BANNED);
    userRepo.save(u);
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/unban")
  @Transactional
  public ApiResult<Void> unban(@PathVariable long id) {
    UserAccount u = userRepo.findById(id).orElseThrow(() -> new BizException("User not found"));
    u.setStatus(UserStatus.ACTIVE);
    userRepo.save(u);
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/credit")
  @Transactional
  public ApiResult<Void> setCredit(@PathVariable long id, @Valid @RequestBody CreditReq req) {
    UserAccount u = userRepo.findById(id).orElseThrow(() -> new BizException("User not found"));
    u.setCreditScore(req.creditScore());
    userRepo.save(u);
    return ApiResult.ok(null);
  }

  @PostMapping("/{id}/wallet/credit")
  @Transactional
  public ApiResult<Void> creditWallet(@PathVariable long id, @Valid @RequestBody AdjustBalanceReq req) {
    UserAccount u = userRepo.findById(id).orElseThrow(() -> new BizException("User not found"));
    // Dev-only adjustment: treat as reward credit (so it has flow trail).
    walletService.creditTaskReward(u, req.amount(), "ADMIN_CREDIT");
    return ApiResult.ok(null);
  }
}
