package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.config.AppProperties;
import com.leiyang.anontask.dto.mp.WalletResponse;
import com.leiyang.anontask.dto.mp.WithdrawRequest;
import com.leiyang.anontask.security.SecurityUtil;
import com.leiyang.anontask.service.ConfigService;
import com.leiyang.anontask.service.UserService;
import com.leiyang.anontask.service.WalletService;
import com.leiyang.anontask.service.WxPayService;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mp/wallet")
public class MpWalletController {
  private final UserService userService;
  private final WalletService walletService;
  private final ConfigService configService;
  private final AppProperties props;
  private final WxPayService wxPayService;

  public MpWalletController(UserService userService, WalletService walletService,
      ConfigService configService, AppProperties props, WxPayService wxPayService) {
    this.userService = userService;
    this.walletService = walletService;
    this.configService = configService;
    this.props = props;
    this.wxPayService = wxPayService;
  }

  @GetMapping("")
  public ApiResult<WalletResponse> wallet() {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    var a = walletService.getOrCreate(user);
    return ApiResult.ok(new WalletResponse(
        a.getBalance(),
        a.getFrozenAmount(),
        a.getTotalIncome(),
        user.getWithdrawQrCodeUrl()
    ));
  }

  public record WithdrawResp(String applyNo, String status) {}
  public record SaveQrReq(@NotBlank(message = "qrCodeUrl is required") String qrCodeUrl) {}
  public record RechargeConfig(String channel, String qrCodeUrl, String name) {}
  public record PrepayResp(String timeStamp, String nonceStr, String packageVal, String signType, String paySign) {}
  public record NativePrepayResp(String outTradeNo, String codeUrl) {}
  public record RechargeStatusResp(String status) {}
  public record RechargeReq(@DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount) {}
  public record FlowItem(String type, BigDecimal amount, String bizNo, String status, Instant createdAt, String label) {}

  public record MyWithdrawItem(
      String applyNo,
      BigDecimal amount,
      String channel,
      String status,
      String auditReason,
      Instant createdAt,
      String qrCodeUrl,
      String paidProofUrl,
      String payRemark,
      Instant paidAt
  ) {}

  @PostMapping("/withdraw")
  public ApiResult<WithdrawResp> withdraw(@Valid @RequestBody WithdrawRequest req) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    var w = walletService.applyWithdraw(user, req.amount(), req.channel(), req.qrCodeUrl());
    return ApiResult.ok(new WithdrawResp(w.getApplyNo(), w.getAuditStatus().name()));
  }

  @PostMapping("/withdraw-qr")
  public ApiResult<Void> saveWithdrawQr(@Valid @RequestBody SaveQrReq req) {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    walletService.saveWithdrawQrCode(user, req.qrCodeUrl());
    return ApiResult.ok(null);
  }

  @GetMapping("/my-withdraws")
  public ApiResult<List<MyWithdrawItem>> myWithdraws() {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    var list = walletService.myWithdraws(user).stream()
        .map(w -> new MyWithdrawItem(
            w.getApplyNo(),
            w.getAmount(),
            w.getChannel(),
            w.getAuditStatus().name(),
            w.getAuditReason(),
            w.getCreatedAt(),
            safe(w.getQrCodeUrl()),
            safe(w.getPaidProofUrl()),
            safe(w.getPayRemark()),
            w.getPaidAt()
        ))
        .toList();
    return ApiResult.ok(list);
  }

  @GetMapping("/recharge-config")
  public ApiResult<RechargeConfig> rechargeConfig() {
    // WXPAY_ENABLED + 后台开关同时打开 → Native 扫码支付；否则 → 展示收款码
    boolean nativeEnabled = wxPayService.isEnabled()
        && configService.getBoolean("wxpay.recharge.enabled", false);
    String channel = nativeEnabled ? "WXPAY_NATIVE" : "WECHAT";
    return ApiResult.ok(new RechargeConfig(
        channel,
        configService.getString("recharge.wechat.qr_url", ""),
        configService.getString("recharge.wechat.name", "叼瓜赖圈")
    ));
  }

  /** 微信支付充值：创建预支付订单，返回小程序调起支付所需参数 */
  @PostMapping("/recharge/prepay")
  public ApiResult<PrepayResp> rechargePrepay(@Valid @RequestBody RechargeReq req) {
    if (!wxPayService.isEnabled()) {
      throw new BizException("微信支付未启用，请联系管理员");
    }
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    PrepayWithRequestPaymentResponse resp = wxPayService.createJsapiRechargeOrder(user, req.amount());
    return ApiResult.ok(new PrepayResp(
        resp.getTimeStamp(),
        resp.getNonceStr(),
        resp.getPackageVal(),
        resp.getSignType() == null ? "RSA" : resp.getSignType().toString(),
        resp.getPaySign()
    ));
  }

  /** Native 扫码支付：创建订单并返回 code_url，前端转成二维码后轮询状态 */
  @PostMapping("/recharge/native-prepay")
  public ApiResult<NativePrepayResp> nativeRechargePrepay(@Valid @RequestBody RechargeReq req) {
    if (!wxPayService.isEnabled()) {
      throw new BizException("微信支付未启用，请联系管理员");
    }
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    var resp = wxPayService.createNativeRechargeOrder(user, req.amount());
    return ApiResult.ok(new NativePrepayResp(resp.outTradeNo(), resp.codeUrl()));
  }

  /** 查询充值订单状态，供前端轮询使用 */
  @GetMapping("/recharge/status/{outTradeNo}")
  public ApiResult<RechargeStatusResp> rechargeStatus(@PathVariable String outTradeNo) {
    long userId = SecurityUtil.requireMpUserId();
    var order = wxPayService.findRechargeOrder(outTradeNo);
    if (!order.getUser().getId().equals(userId)) {
      throw new BizException("订单不存在");
    }
    return ApiResult.ok(new RechargeStatusResp(order.getStatus()));
  }

  @PostMapping("/recharge/mock-success")
  public ApiResult<Void> mockRecharge(@Valid @RequestBody RechargeReq req) {
    if (!props.mp().allowMockLogin()) {
      throw new BizException("Mock recharge is disabled");
    }
    long userId = SecurityUtil.requireMpUserId();
    walletService.mockRecharge(userService.requireUser(userId), req.amount());
    return ApiResult.ok(null);
  }

  @GetMapping("/flows")
  public ApiResult<List<FlowItem>> flows() {
    long userId = SecurityUtil.requireMpUserId();
    var user = userService.requireUser(userId);
    var list = walletService.myFlows(user).stream()
        .map(f -> new FlowItem(
            f.getFlowType().name(),
            f.getAmount(),
            f.getBizNo(),
            f.getStatus().name(),
            f.getCreatedAt(),
            flowLabel(f.getFlowType().name())
        ))
        .toList();
    return ApiResult.ok(list);
  }

  private static String flowLabel(String type) {
    return switch (type) {
      case "RECHARGE" -> "充值入账";
      case "TASK_REWARD" -> "任务收入";
      case "MUSIC_TIP_IN" -> "音乐打赏收入";
      case "MUSIC_TIP_OUT" -> "音乐打赏支出";
      case "MUSIC_GENERATE_PAY" -> "AI音乐生成扣款";
      case "MUSIC_PACKAGE_BUY" -> "AI音乐套餐购买";
      case "TASK_PUBLISH_FREEZE" -> "发布任务冻结";
      case "TASK_PUBLISH_SETTLE" -> "发布任务扣款";
      case "TASK_PUBLISH_UNFREEZE" -> "发布任务退回";
      case "WITHDRAW_APPLY_FREEZE" -> "提现冻结";
      case "WITHDRAW_PAID" -> "提现打款";
      case "WITHDRAW_REJECT_UNFREEZE" -> "提现退回";
      default -> type;
    };
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim();
  }
}
