package com.leiyang.anontask.controller.publicapi;

import com.leiyang.anontask.domain.RechargeOrder;
import com.leiyang.anontask.service.WalletService;
import com.leiyang.anontask.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信支付回调接口，路径需在 SecurityConfig 中放开，且无需鉴权。
 * 微信回调规范：成功返回 HTTP 200 + {"code":"SUCCESS"}，否则微信会重试。
 */
@RestController
@RequestMapping("/api/public/wxpay")
public class WxPayCallbackController {
  private static final Logger log = LoggerFactory.getLogger(WxPayCallbackController.class);

  private final WxPayService wxPayService;
  private final WalletService walletService;

  public WxPayCallbackController(WxPayService wxPayService, WalletService walletService) {
    this.wxPayService = wxPayService;
    this.walletService = walletService;
  }

  /** 充值支付结果回调 */
  @PostMapping("/recharge-notify")
  public ResponseEntity<Map<String, String>> rechargeNotify(HttpServletRequest request)
      throws IOException {
    Map<String, String> headers = extractWxHeaders(request);
    String body = new String(request.getInputStream().readAllBytes());
    log.info("wxpay_recharge_notify_received headers={}", headers.keySet());

    try {
      Transaction tx = wxPayService.parseRechargeCallback(headers, body);
      String outTradeNo = tx.getOutTradeNo();
      String tradeState = tx.getTradeState() == null ? "" : tx.getTradeState().name();
      log.info("wxpay_recharge_notify outTradeNo={} tradeState={} transactionId={}",
          outTradeNo, tradeState, tx.getTransactionId());

      if (!"SUCCESS".equals(tradeState)) {
        // 非成功状态不处理，直接告知微信已收到（避免重试）
        return ok();
      }

      RechargeOrder order = wxPayService.findRechargeOrder(outTradeNo);
      if (RechargeOrder.STATUS_SUCCESS.equals(order.getStatus())) {
        return ok(); // 幂等：已处理
      }

      wxPayService.markRechargeSuccess(order, tx.getTransactionId());

      // 分换算回元，入账钱包
      BigDecimal yuan = BigDecimal.valueOf(order.getAmountFen()).divide(BigDecimal.valueOf(100));
      walletService.confirmRecharge(order.getUser(), yuan, outTradeNo);

      log.info("wxpay_recharge_success outTradeNo={} userId={} yuan={}",
          outTradeNo, order.getUser().getId(), yuan);
      return ok();

    } catch (Exception e) {
      log.error("wxpay_recharge_notify_error", e);
      return fail(e.getMessage());
    }
  }

  private static Map<String, String> extractWxHeaders(HttpServletRequest request) {
    Map<String, String> map = new HashMap<>();
    for (String h : new String[]{
        "wechatpay-serial", "wechatpay-nonce",
        "wechatpay-signature", "wechatpay-timestamp"}) {
      String v = request.getHeader(h);
      if (v != null) map.put(h, v);
    }
    return map;
  }

  private static ResponseEntity<Map<String, String>> ok() {
    return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
  }

  private static ResponseEntity<Map<String, String>> fail(String msg) {
    return ResponseEntity.ok(Map.of("code", "FAIL", "message", msg == null ? "处理失败" : msg));
  }
}
