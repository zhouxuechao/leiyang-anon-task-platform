package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.config.AppProperties;
import com.leiyang.anontask.domain.RechargeOrder;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.repo.RechargeOrderRepository;
import com.leiyang.anontask.util.NoGenerator;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.transferbatch.TransferBatchService;
import com.wechat.pay.java.service.transferbatch.model.InitiateBatchTransferRequest;
import com.wechat.pay.java.service.transferbatch.model.InitiateBatchTransferResponse;
import com.wechat.pay.java.service.transferbatch.model.TransferDetailInput;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WxPayService {
  private static final Logger log = LoggerFactory.getLogger(WxPayService.class);

  private final AppProperties props;
  private final RechargeOrderRepository rechargeOrderRepo;
  private volatile Config wxConfig;

  public WxPayService(AppProperties props, RechargeOrderRepository rechargeOrderRepo) {
    this.props = props;
    this.rechargeOrderRepo = rechargeOrderRepo;
  }

  public boolean isEnabled() {
    AppProperties.WxPay c = props.wxpay();
    return c != null && c.enabled();
  }

  // ── 充值 ────────────────────────────────────────────────────────────────────

  /** 创建 JSAPI 预支付订单，返回给小程序调起支付所需参数 */
  @Transactional
  public PrepayWithRequestPaymentResponse createJsapiRechargeOrder(UserAccount user, BigDecimal amount) {
    int amountFen = toFen(amount);
    String outTradeNo = NoGenerator.gen("R");

    RechargeOrder order = new RechargeOrder();
    order.setOutTradeNo(outTradeNo);
    order.setUser(user);
    order.setAmountFen(amountFen);
    order.setStatus(RechargeOrder.STATUS_PENDING);
    rechargeOrderRepo.save(order);

    AppProperties.WxPay cfg = props.wxpay();
    PrepayRequest req = new PrepayRequest();
    req.setAppid(props.mp().appId());
    req.setMchid(cfg.mchId());
    req.setDescription("钱包充值");
    req.setOutTradeNo(outTradeNo);
    req.setNotifyUrl(cfg.notifyDomain() + "/api/public/wxpay/recharge-notify");

    Amount amountObj = new Amount();
    amountObj.setTotal(amountFen);
    req.setAmount(amountObj);

    Payer payer = new Payer();
    payer.setOpenid(user.getOpenId());
    req.setPayer(payer);

    try {
      JsapiServiceExtension service = new JsapiServiceExtension.Builder()
          .config(getConfig())
          .build();
      PrepayWithRequestPaymentResponse resp = service.prepayWithRequestPayment(req);
      log.info("wxpay_jsapi_prepay outTradeNo={} userId={} amountFen={}", outTradeNo, user.getId(), amountFen);
      return resp;
    } catch (Exception e) {
      log.error("wxpay_jsapi_prepay_failed outTradeNo={}", outTradeNo, e);
      throw new BizException("创建微信支付订单失败：" + e.getMessage());
    }
  }

  /** 解析充值回调通知，返回微信 Transaction 对象 */
  public Transaction parseRechargeCallback(Map<String, String> headers, String body) {
    RequestParam requestParam = new RequestParam.Builder()
        .serialNumber(headers.get("wechatpay-serial"))
        .nonce(headers.get("wechatpay-nonce"))
        .signature(headers.get("wechatpay-signature"))
        .timestamp(headers.get("wechatpay-timestamp"))
        .body(body)
        .build();
    try {
      NotificationParser parser = new NotificationParser(
          (com.wechat.pay.java.core.notification.NotificationConfig) getConfig());
      return parser.parse(requestParam, Transaction.class);
    } catch (Exception e) {
      log.error("wxpay_callback_verify_failed", e);
      throw new BizException("回调验签失败");
    }
  }

  /** 根据 outTradeNo 查充值订单，供回调使用 */
  public RechargeOrder findRechargeOrder(String outTradeNo) {
    return rechargeOrderRepo.findByOutTradeNo(outTradeNo)
        .orElseThrow(() -> new BizException("充值订单不存在: " + outTradeNo));
  }

  /** 标记充值订单已成功（回调成功后调用） */
  @Transactional
  public RechargeOrder markRechargeSuccess(RechargeOrder order, String wxTransactionId) {
    if (RechargeOrder.STATUS_SUCCESS.equals(order.getStatus())) {
      return order; // 幂等
    }
    order.setStatus(RechargeOrder.STATUS_SUCCESS);
    order.setWxTransactionId(wxTransactionId);
    return rechargeOrderRepo.save(order);
  }

  // ── 提现（商家转账到零钱）──────────────────────────────────────────────────

  /**
   * 向用户微信零钱发起转账，返回微信 batchId。
   * 需要商户开通「商家转账到零钱」功能，否则调用会报错。
   *
   * @param openId     用户 openId
   * @param amount     提现金额（元）
   * @param outBatchNo 商户批次号（唯一）
   * @param remark     转账备注，展示给用户
   */
  public String transferToUserWallet(String openId, BigDecimal amount, String outBatchNo, String remark) {
    int amountFen = toFen(amount);
    String outDetailNo = outBatchNo + "D1";

    TransferDetailInput detail = new TransferDetailInput();
    detail.setOutDetailNo(outDetailNo);
    detail.setTransferAmount(amountFen);
    detail.setTransferRemark(remark);
    detail.setOpenid(openId);

    InitiateBatchTransferRequest req = new InitiateBatchTransferRequest();
    req.setAppid(props.mp().appId());
    req.setOutBatchNo(outBatchNo);
    req.setBatchName("平台提现");
    req.setBatchRemark(remark);
    req.setTotalAmount(amountFen);
    req.setTotalNum(1);
    req.setTransferDetailList(List.of(detail));

    try {
      TransferBatchService service = new TransferBatchService.Builder()
          .config(getConfig())
          .build();
      InitiateBatchTransferResponse resp = service.initiateBatchTransfer(req);
      log.info("wxpay_transfer_initiated outBatchNo={} openId={} amountFen={} batchId={}",
          outBatchNo, openId, amountFen, resp.getBatchId());
      return resp.getBatchId();
    } catch (Exception e) {
      log.error("wxpay_transfer_failed outBatchNo={}", outBatchNo, e);
      throw new BizException("微信转账失败：" + e.getMessage());
    }
  }

  // ── 内部 ─────────────────────────────────────────────────────────────────

  private Config getConfig() {
    if (wxConfig != null) return wxConfig;
    synchronized (this) {
      if (wxConfig != null) return wxConfig;
      AppProperties.WxPay cfg = props.wxpay();
      if (cfg == null || !cfg.enabled()) throw new BizException("微信支付未启用");
      try {
        String privateKey = resolvePrivateKey(cfg);
        wxConfig = new RSAAutoCertificateConfig.Builder()
            .merchantId(cfg.mchId())
            .privateKey(privateKey)
            .merchantSerialNumber(cfg.serialNo())
            .apiV3Key(cfg.apiV3Key())
            .build();
        log.info("wxpay_config_initialized mchId={}", cfg.mchId());
        return wxConfig;
      } catch (BizException e) {
        throw e;
      } catch (Exception e) {
        throw new BizException("微信支付初始化失败：" + e.getMessage());
      }
    }
  }

  private String resolvePrivateKey(AppProperties.WxPay cfg) throws Exception {
    String path = cfg.privateKeyPath();
    if (path != null && !path.isBlank()) {
      return Files.readString(Paths.get(path));
    }
    String key = cfg.privateKey();
    if (key != null && !key.isBlank()) {
      // 环境变量中 \n 可能是字面量，转换为真正的换行
      return key.replace("\\n", "\n");
    }
    throw new BizException("微信支付私钥未配置（WXPAY_PRIVATE_KEY_PATH 或 WXPAY_PRIVATE_KEY 二选一）");
  }

  private static int toFen(BigDecimal yuan) {
    return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
  }
}
