package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.UserAccount;
import com.leiyang.anontask.domain.WalletAccount;
import com.leiyang.anontask.domain.WalletFlow;
import com.leiyang.anontask.domain.WithdrawApply;
import com.leiyang.anontask.domain.enums.FlowStatus;
import com.leiyang.anontask.domain.enums.WalletFlowType;
import com.leiyang.anontask.domain.enums.WithdrawStatus;
import com.leiyang.anontask.repo.WalletAccountRepository;
import com.leiyang.anontask.repo.WalletFlowRepository;
import com.leiyang.anontask.repo.WithdrawApplyRepository;
import com.leiyang.anontask.repo.UserAccountRepository;
import com.leiyang.anontask.util.NoGenerator;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
  private static final Logger log = LoggerFactory.getLogger(WalletService.class);

  private final WalletAccountRepository accountRepo;
  private final WalletFlowRepository flowRepo;
  private final WithdrawApplyRepository withdrawRepo;
  private final UserAccountRepository userRepo;
  private final MessageService messageService;
  private final RedisSupportService redisSupport;

  public WalletService(
      WalletAccountRepository accountRepo,
      WalletFlowRepository flowRepo,
      WithdrawApplyRepository withdrawRepo,
      UserAccountRepository userRepo,
      MessageService messageService,
      RedisSupportService redisSupport
  ) {
    this.accountRepo = accountRepo;
    this.flowRepo = flowRepo;
    this.withdrawRepo = withdrawRepo;
    this.userRepo = userRepo;
    this.messageService = messageService;
    this.redisSupport = redisSupport;
  }

  @Transactional
  public WalletAccount getOrCreate(UserAccount user) {
    return accountRepo.findByUser(user).orElseGet(() -> {
      WalletAccount a = new WalletAccount();
      a.setUser(user);
      a.setBalance(BigDecimal.ZERO);
      a.setFrozenAmount(BigDecimal.ZERO);
      a.setTotalIncome(BigDecimal.ZERO);
      return accountRepo.save(a);
    });
  }

  @Transactional
  public void creditTaskReward(UserAccount user, BigDecimal amount, String bizNo) {
    if (amount == null || amount.signum() <= 0) {
      throw new BizException("Invalid amount");
    }
    WalletAccount a = getOrCreate(user);
    a.setBalance(a.getBalance().add(amount));
    a.setTotalIncome(a.getTotalIncome().add(amount));
    accountRepo.save(a);

    WalletFlow f = new WalletFlow();
    f.setUser(user);
    f.setFlowType(WalletFlowType.TASK_REWARD);
    f.setAmount(amount);
    f.setBizNo(bizNo);
    f.setStatus(FlowStatus.SUCCESS);
    f.setCreatedAt(Instant.now());
    flowRepo.save(f);
    log.info("wallet_credit_task_reward userId={} amount={} bizNo={}", user.getId(), amount, bizNo);
  }

  @Transactional
  public void mockRecharge(UserAccount user, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) throw new BizException("Invalid amount");
    WalletAccount a = getOrCreate(user);
    a.setBalance(a.getBalance().add(amount));
    accountRepo.save(a);
    WalletFlow f = newFlow(user, WalletFlowType.RECHARGE, amount, "RECHARGE");
    flowRepo.save(f);
    log.info("wallet_recharge userId={} amount={}", user.getId(), amount);
  }

  /** 微信支付充值回调成功后调用，将金额入账余额 */
  @Transactional
  public void confirmRecharge(UserAccount user, BigDecimal amount, String outTradeNo) {
    if (amount == null || amount.signum() <= 0) throw new BizException("Invalid recharge amount");
    WalletAccount a = getOrCreate(user);
    a.setBalance(a.getBalance().add(amount));
    accountRepo.save(a);
    flowRepo.save(newFlow(user, WalletFlowType.RECHARGE, amount, outTradeNo));
    messageService.send(user, "WALLET", "充值成功", "钱包充值 ¥" + amount + " 已到账，订单号：" + outTradeNo);
    log.info("wallet_confirm_recharge userId={} amount={} outTradeNo={}", user.getId(), amount, outTradeNo);
  }

  @Transactional
  public void debitForMusic(UserAccount user, BigDecimal amount, WalletFlowType type, String bizNo) {
    if (amount == null || amount.signum() <= 0) throw new BizException("Invalid amount");
    WalletAccount a = getOrCreate(user);
    if (a.getBalance().compareTo(amount) < 0) throw new BizException("Insufficient balance");
    a.setBalance(a.getBalance().subtract(amount));
    accountRepo.save(a);
    flowRepo.save(newFlow(user, type, amount, bizNo));
    log.info("wallet_debit_music userId={} amount={} type={} bizNo={}", user.getId(), amount, type, bizNo);
  }

  @Transactional
  public void freezeForTaskPublish(UserAccount user, BigDecimal amount, String taskNo) {
    if (amount == null || amount.signum() <= 0) throw new BizException("Invalid amount");
    WalletAccount a = getOrCreate(user);
    if (a.getBalance().compareTo(amount) < 0) throw new BizException("Insufficient balance");
    a.setBalance(a.getBalance().subtract(amount));
    a.setFrozenAmount(a.getFrozenAmount().add(amount));
    accountRepo.save(a);
    flowRepo.save(newFlow(user, WalletFlowType.TASK_PUBLISH_FREEZE, amount, taskNo));
    log.info("wallet_freeze_task_publish userId={} amount={} taskNo={}", user.getId(), amount, taskNo);
  }

  @Transactional
  public void settleTaskPublishFreeze(UserAccount user, BigDecimal amount, String taskNo) {
    if (amount == null || amount.signum() <= 0) return;
    WalletAccount a = getOrCreate(user);
    BigDecimal release = a.getFrozenAmount().min(amount);
    if (release.signum() <= 0) return;
    a.setFrozenAmount(a.getFrozenAmount().subtract(release));
    accountRepo.save(a);
    flowRepo.save(newFlow(user, WalletFlowType.TASK_PUBLISH_SETTLE, release, taskNo));
  }

  @Transactional
  public void unfreezeTaskPublish(UserAccount user, BigDecimal amount, String taskNo) {
    if (amount == null || amount.signum() <= 0) return;
    WalletAccount a = getOrCreate(user);
    BigDecimal release = a.getFrozenAmount().min(amount);
    if (release.signum() <= 0) return;
    a.setFrozenAmount(a.getFrozenAmount().subtract(release));
    a.setBalance(a.getBalance().add(release));
    accountRepo.save(a);
    flowRepo.save(newFlow(user, WalletFlowType.TASK_PUBLISH_UNFREEZE, release, taskNo));
  }

  @Transactional
  public WithdrawApply applyWithdraw(UserAccount user, BigDecimal amount, String channel, String qrCodeUrl) {
    if (!redisSupport.setIfAbsent("lock:wallet:withdraw:user:" + user.getId(), String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(30))) {
      throw new BizException("提现正在提交，请勿重复点击");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new BizException("Invalid withdraw amount");
    }
    String qr = qrCodeUrl == null ? "" : qrCodeUrl.trim();
    String savedQr = user.getWithdrawQrCodeUrl() == null ? "" : user.getWithdrawQrCodeUrl().trim();
    if (qr.isEmpty() && savedQr.isEmpty()) throw new BizException("WeChat QR code is required");
    if (qr.isEmpty()) {
      qr = savedQr;
    } else if (!qr.equals(savedQr)) {
      user.setWithdrawQrCodeUrl(qr);
      userRepo.save(user);
    }
    WalletAccount a = getOrCreate(user);
    if (a.getBalance().compareTo(amount) < 0) {
      throw new BizException("Insufficient balance");
    }
    a.setBalance(a.getBalance().subtract(amount));
    a.setFrozenAmount(a.getFrozenAmount().add(amount));
    accountRepo.save(a);

    WithdrawApply w = new WithdrawApply();
    w.setApplyNo(NoGenerator.gen("W"));
    w.setUser(user);
    w.setAmount(amount);
    w.setChannel(channel);
    w.setQrCodeUrl(qr);
    w.setAuditStatus(WithdrawStatus.PENDING);
    withdrawRepo.save(w);

    WalletFlow f = new WalletFlow();
    f.setUser(user);
    f.setFlowType(WalletFlowType.WITHDRAW_APPLY_FREEZE);
    f.setAmount(amount);
    f.setBizNo(w.getApplyNo());
    f.setStatus(FlowStatus.SUCCESS);
    f.setCreatedAt(Instant.now());
    flowRepo.save(f);

    return w;
  }

  @Transactional
  public void saveWithdrawQrCode(UserAccount user, String qrCodeUrl) {
    String qr = qrCodeUrl == null ? "" : qrCodeUrl.trim();
    if (qr.isEmpty()) {
      throw new BizException("WeChat QR code is required");
    }
    if (qr.equals(user.getWithdrawQrCodeUrl() == null ? "" : user.getWithdrawQrCodeUrl().trim())) return;
    user.setWithdrawQrCodeUrl(qr);
    userRepo.save(user);
  }

  @Transactional
  public void adminPayWithdraw(WithdrawApply w) {
    adminPayWithdraw(w, "", "");
  }

  @Transactional
  public void adminPayWithdraw(WithdrawApply w, String paidProofUrl, String payRemark) {
    if (w.getAuditStatus() != WithdrawStatus.PENDING) {
      throw new BizException("Withdraw is not pending");
    }
    String proof = paidProofUrl == null ? "" : paidProofUrl.trim();
    if (proof.isEmpty()) {
      throw new BizException("Paid proof is required");
    }
    WalletAccount a = getOrCreate(w.getUser());
    if (a.getFrozenAmount().compareTo(w.getAmount()) < 0) {
      // Data healing: old records may miss freeze flow.补齐冻结后继续闭环。
      BigDecimal gap = w.getAmount().subtract(a.getFrozenAmount());
      if (a.getBalance().compareTo(gap) < 0) {
        throw new BizException("Insufficient balance to settle this withdraw");
      }
      a.setBalance(a.getBalance().subtract(gap));
      a.setFrozenAmount(a.getFrozenAmount().add(gap));
    }
    a.setFrozenAmount(a.getFrozenAmount().subtract(w.getAmount()));
    accountRepo.save(a);

    w.setAuditStatus(WithdrawStatus.PAID);
    w.setPaidProofUrl(proof);
    w.setPayRemark(payRemark == null ? "" : payRemark.trim());
    w.setPaidAt(Instant.now());
    withdrawRepo.save(w);

    WalletFlow f = new WalletFlow();
    f.setUser(w.getUser());
    f.setFlowType(WalletFlowType.WITHDRAW_PAID);
    f.setAmount(w.getAmount());
    f.setBizNo(w.getApplyNo());
    f.setStatus(FlowStatus.SUCCESS);
    f.setCreatedAt(Instant.now());
    flowRepo.save(f);
    messageService.send(w.getUser(), "AUDIT", "提现已打款", "提现 " + w.getApplyNo() + " 已打款 ¥" + w.getAmount() + "，请查看收款账户。");
  }

  @Transactional
  public void adminRejectWithdraw(WithdrawApply w, String reason) {
    if (w.getAuditStatus() != WithdrawStatus.PENDING) {
      throw new BizException("Withdraw is not pending");
    }
    WalletAccount a = getOrCreate(w.getUser());
    BigDecimal release = a.getFrozenAmount().min(w.getAmount());
    if (release.signum() <= 0) throw new BizException("No frozen amount to release");
    a.setFrozenAmount(a.getFrozenAmount().subtract(release));
    a.setBalance(a.getBalance().add(release));
    accountRepo.save(a);

    w.setAuditStatus(WithdrawStatus.REJECTED);
    w.setAuditReason(reason);
    withdrawRepo.save(w);

    WalletFlow f = new WalletFlow();
    f.setUser(w.getUser());
    f.setFlowType(WalletFlowType.WITHDRAW_REJECT_UNFREEZE);
    f.setAmount(release);
    f.setBizNo(w.getApplyNo());
    f.setStatus(FlowStatus.SUCCESS);
    f.setCreatedAt(Instant.now());
    flowRepo.save(f);
    messageService.send(w.getUser(), "AUDIT", "提现审核驳回", "提现 " + w.getApplyNo() + " 已驳回，冻结金额已退回余额。原因：" + (reason == null ? "" : reason));
  }

  @Transactional
  public List<WithdrawApply> myWithdraws(UserAccount user) {
    return withdrawRepo.findTop50ByUserOrderByCreatedAtDesc(user);
  }

  public List<WalletFlow> myFlows(UserAccount user) {
    return flowRepo.findTop50ByUserOrderByCreatedAtDesc(user);
  }

  private static WalletFlow newFlow(UserAccount user, WalletFlowType type, BigDecimal amount, String bizNo) {
    WalletFlow f = new WalletFlow();
    f.setUser(user);
    f.setFlowType(type);
    f.setAmount(amount);
    f.setBizNo(bizNo);
    f.setStatus(FlowStatus.SUCCESS);
    f.setCreatedAt(Instant.now());
    return f;
  }
}
