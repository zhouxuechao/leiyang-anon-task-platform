package com.leiyang.anontask.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Jwt jwt,
    Mp mp,
    Limits limits,
    Upload upload,
    Cors cors,
    WxPay wxpay
) {
  public record Jwt(String issuer, String secret, long expireMinutes) {}

  public record Mp(boolean allowMockLogin, String appId, String appSecret) {}

  public record Limits(int maxOngoingOrders) {}

  public record Upload(String dir, Oss oss) {}

  public record Cors(String allowedOrigins) {}

  /** 微信支付配置，enabled=false 时其余字段可留空 */
  public record WxPay(
      boolean enabled,
      String mchId,           // 商户号
      String apiV3Key,        // API v3 密钥（32字节）
      String serialNo,        // 商户证书序列号
      String privateKeyPath,  // apiclient_key.pem 文件路径（与 privateKey 二选一）
      String privateKey,      // PEM 内容（环境变量注入，\n 用字面量 \n 即可）
      String notifyDomain     // 回调域名，如 https://your-domain.com（结尾不加斜杠）
  ) {}

  public record Oss(
      boolean enabled,
      String endpoint,
      String bucket,
      String accessKeyId,
      String accessKeySecret,
      String baseUrl,
      String dir,
      boolean publicRead,
      boolean signedUrl,
      long signedUrlExpireSeconds
  ) {}
}
