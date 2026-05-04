package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.enums.WithdrawStatus;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.dto.admin.AuditRequest;
import com.leiyang.anontask.repo.WithdrawApplyRepository;
import com.leiyang.anontask.service.WalletService;
import com.leiyang.anontask.service.WxPayService;
import com.leiyang.anontask.util.NoGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/admin/withdraws")
public class AdminWithdrawController {
  private final WithdrawApplyRepository repo;
  private final WalletService walletService;
  private final WxPayService wxPayService;

  public AdminWithdrawController(WithdrawApplyRepository repo, WalletService walletService, WxPayService wxPayService) {
    this.repo = repo;
    this.walletService = walletService;
    this.wxPayService = wxPayService;
  }

  public record WithdrawItem(
      String applyNo,
      String userOpenId,
      String amount,
      String channel,
      String status,
      String createdAt,
      String qrCodeUrl
  ) {}
  public record WithdrawDetail(
      String applyNo,
      String userOpenId,
      String amount,
      String channel,
      String status,
      String createdAt,
      String qrCodeUrl,
      String paidProofUrl,
      String payRemark,
      String paidAt
  ) {}
  public record WithdrawStats(
      long pendingCount,
      String pendingAmount,
      long paidCount,
      String paidAmount,
      long todayPaidCount,
      String todayPaidAmount
  ) {}
  public record WithdrawRecordItem(
      String applyNo,
      String userOpenId,
      String amount,
      String channel,
      String status,
      String createdAt,
      String qrCodeUrl,
      String paidProofUrl,
      String payRemark,
      String paidAt
  ) {}
  public record PayCompleteRequest(
      @NotBlank(message = "paidProofUrl is required") String paidProofUrl,
      String payRemark
  ) {}

  @GetMapping("/pending")
  public ApiResult<List<WithdrawItem>> pending() {
    var list = repo.findTop50ByAuditStatusOrderByCreatedAtDesc(WithdrawStatus.PENDING).stream()
        .map(w -> new WithdrawItem(
            w.getApplyNo(),
            w.getUser().getOpenId(),
            w.getAmount().toPlainString(),
            w.getChannel(),
            w.getAuditStatus().name(),
            w.getCreatedAt().toString(),
            w.getQrCodeUrl() == null ? "" : w.getQrCodeUrl().trim()
        ))
        .toList();
    return ApiResult.ok(list);
  }

  @GetMapping("/stats")
  public ApiResult<WithdrawStats> stats() {
    Instant todayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
    long pendingCount = repo.countByAuditStatus(WithdrawStatus.PENDING);
    BigDecimal pendingAmount = repo.sumAmountByAuditStatus(WithdrawStatus.PENDING);
    long paidCount = repo.countByAuditStatus(WithdrawStatus.PAID);
    BigDecimal paidAmount = repo.sumAmountByAuditStatus(WithdrawStatus.PAID);
    long todayPaidCount = repo.countByAuditStatusAndPaidAtGreaterThanEqual(WithdrawStatus.PAID, todayStart);
    BigDecimal todayPaidAmount = repo.sumAmountByAuditStatusAndPaidAtGreaterThanEqual(WithdrawStatus.PAID, todayStart);
    return ApiResult.ok(new WithdrawStats(
        pendingCount,
        money(pendingAmount),
        paidCount,
        money(paidAmount),
        todayPaidCount,
        money(todayPaidAmount)
    ));
  }

  @GetMapping("/records")
  public ApiResult<PageResponse<WithdrawRecordItem>> records(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "") String q
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(100, Math.max(1, size));
    WithdrawStatus s = parseStatus(status);
    var p = repo.adminSearch(s, trim(q), PageRequest.of(safePage - 1, safeSize));
    var items = p.getContent().stream().map(w -> new WithdrawRecordItem(
        w.getApplyNo(),
        w.getUser().getOpenId(),
        money(w.getAmount()),
        w.getChannel(),
        w.getAuditStatus().name(),
        w.getCreatedAt() == null ? "" : w.getCreatedAt().toString(),
        safe(w.getQrCodeUrl()),
        safe(w.getPaidProofUrl()),
        safe(w.getPayRemark()),
        w.getPaidAt() == null ? "" : w.getPaidAt().toString()
    )).toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), items));
  }

  @GetMapping("/{applyNo}")
  public ApiResult<WithdrawDetail> detail(@PathVariable String applyNo) {
    var w = repo.findByApplyNo(applyNo).orElseThrow(() -> new BizException("Withdraw not found"));
    return ApiResult.ok(new WithdrawDetail(
        w.getApplyNo(),
        w.getUser().getOpenId(),
        w.getAmount().toPlainString(),
        w.getChannel(),
        w.getAuditStatus().name(),
        w.getCreatedAt().toString(),
        w.getQrCodeUrl() == null ? "" : w.getQrCodeUrl().trim(),
        w.getPaidProofUrl() == null ? "" : w.getPaidProofUrl().trim(),
        w.getPayRemark() == null ? "" : w.getPayRemark().trim(),
        w.getPaidAt() == null ? "" : w.getPaidAt().toString()
    ));
  }

  @PostMapping("/{applyNo}/pay-complete")
  public ApiResult<Void> payComplete(@PathVariable String applyNo, @Valid @RequestBody PayCompleteRequest req) {
    var w = repo.findByApplyNo(applyNo).orElseThrow(() -> new BizException("Withdraw not found"));
    walletService.adminPayWithdraw(w, req.paidProofUrl(), req.payRemark());
    return ApiResult.ok(null);
  }

  public record AutoPayResult(String batchId) {}

  /**
   * 一键自动打款：调微信「商家转账到零钱」API，成功后自动完结提现单。
   * 需要商户开通该功能，且 WXPAY_ENABLED=true。
   */
  @PostMapping("/{applyNo}/auto-pay")
  public ApiResult<AutoPayResult> autoPay(@PathVariable String applyNo) {
    if (!wxPayService.isEnabled()) {
      throw new BizException("微信支付未启用，请配置 WXPAY_ENABLED=true");
    }
    var w = repo.findByApplyNo(applyNo).orElseThrow(() -> new BizException("Withdraw not found"));
    if (w.getAuditStatus() != WithdrawStatus.PENDING) {
      throw new BizException("该提现单不处于待处理状态");
    }
    String openId = w.getUser().getOpenId();
    if (openId == null || openId.isBlank()) {
      throw new BizException("该用户无微信 openId，无法自动打款");
    }
    String outBatchNo = NoGenerator.gen("TB");
    String remark = "提现到账 " + w.getApplyNo();
    String batchId = wxPayService.transferToUserWallet(openId, w.getAmount(), outBatchNo, remark);
    // 自动打款成功，batchId 作为打款凭证写入
    walletService.adminPayWithdraw(w, "wxpay:batchId=" + batchId, remark);
    return ApiResult.ok(new AutoPayResult(batchId));
  }

  @PostMapping("/{applyNo}/audit")
  public ApiResult<Void> audit(@PathVariable String applyNo, @Valid @RequestBody AuditRequest req) {
    var w = repo.findByApplyNo(applyNo).orElseThrow(() -> new BizException("Withdraw not found"));
    if ("PAY".equalsIgnoreCase(req.result()) || "APPROVE".equalsIgnoreCase(req.result())) {
      throw new BizException("Please use pay-complete flow with paid proof");
    } else if ("REJECT".equalsIgnoreCase(req.result())) {
      walletService.adminRejectWithdraw(w, req.reason());
    } else {
      throw new BizException("Invalid audit result");
    }
    return ApiResult.ok(null);
  }

  private static WithdrawStatus parseStatus(String status) {
    String s = trim(status).toUpperCase(Locale.ROOT);
    if (s.isEmpty() || "ALL".equals(s)) return null;
    try {
      return WithdrawStatus.valueOf(s);
    } catch (Exception e) {
      throw new BizException("Invalid withdraw status");
    }
  }

  private static String money(BigDecimal v) {
    if (v == null) return "0.00";
    return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
