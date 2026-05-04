package com.leiyang.anontask.service;

import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.config.AppProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {
  private static final Logger log = LoggerFactory.getLogger(UploadService.class);
  private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
  private final AppProperties props;

  public UploadService(AppProperties props) {
    this.props = props;
  }

  public String save(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BizException("file is required");
    }
    String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    String ext = "";
    int i = original.lastIndexOf('.');
    if (i >= 0 && i < original.length() - 1) {
      ext = original.substring(i);
      if (ext.length() > 10) {
        ext = "";
      }
    }
    String day = LocalDate.now().format(DAY);
    String name = UUID.randomUUID().toString().replace("-", "") + ext;
    String objectKey = buildObjectKey(day, name);

    if (useOss()) {
      log.info("upload_start storage=oss originalName={} size={} contentType={}", original, file.getSize(), file.getContentType());
      return saveToOss(file, objectKey);
    }

    log.info("upload_start storage=local originalName={} size={} contentType={}", original, file.getSize(), file.getContentType());
    return saveToLocal(file, day, name);
  }

  private String saveToLocal(MultipartFile file, String day, String name) {
    Path dir = Path.of(props.upload().dir(), day).toAbsolutePath().normalize();
    try {
      Files.createDirectories(dir);
      Path dest = dir.resolve(name);
      file.transferTo(dest);
      log.info("upload_success storage=local path={}", dest);
      return "/uploads/" + day + "/" + name;
    } catch (IOException e) {
      log.warn("upload_failed storage=local error={}", e.toString());
      throw new BizException("Upload failed");
    }
  }

  private String saveToOss(MultipartFile file, String objectKey) {
    var oss = props.upload().oss();
    String endpoint = trim(oss.endpoint());
    String bucket = trim(oss.bucket());
    String ak = trim(oss.accessKeyId());
    String sk = trim(oss.accessKeySecret());
    if (endpoint.isEmpty() || bucket.isEmpty() || ak.isEmpty() || sk.isEmpty()) {
      throw new BizException("OSS config missing: endpoint/bucket/access key");
    }
    OSS client = new OSSClientBuilder().build(endpoint, ak, sk);
    try (InputStream in = file.getInputStream()) {
      ObjectMetadata meta = new ObjectMetadata();
      if (file.getSize() > 0) {
        meta.setContentLength(file.getSize());
      }
      String ct = trim(file.getContentType());
      if (!ct.isEmpty()) {
        meta.setContentType(ct);
      }
      client.putObject(bucket, objectKey, in, meta);
      boolean forceSigned = oss.signedUrl();
      if (oss.publicRead()) {
        try {
          client.setObjectAcl(bucket, objectKey, CannedAccessControlList.PublicRead);
        } catch (Exception ignore) {
          // Bucket may block ACL writes; then direct public URL could be 403.
          forceSigned = true;
        }
      } else {
        // Private bucket without signed URL will always be inaccessible by direct URL.
        forceSigned = true;
      }
      if (forceSigned) {
        long expire = Math.max(300L, oss.signedUrlExpireSeconds());
        Date expiration = Date.from(Instant.now().plusSeconds(expire));
        log.info("upload_success storage=oss bucket={} key={} signedUrl=true", bucket, objectKey);
        return client.generatePresignedUrl(bucket, objectKey, expiration).toString();
      }
      log.info("upload_success storage=oss bucket={} key={} signedUrl=false", bucket, objectKey);
      return publicUrl(oss, bucket, endpoint, objectKey);
    } catch (Exception e) {
      log.warn("upload_failed storage=oss bucket={} key={} error={}", bucket, objectKey, e.toString());
      throw new BizException("Upload to OSS failed: " + e.getMessage());
    } finally {
      client.shutdown();
    }
  }

  private boolean useOss() {
    var upload = props.upload();
    return upload != null && upload.oss() != null && upload.oss().enabled();
  }

  private String buildObjectKey(String day, String name) {
    String prefix = "";
    var upload = props.upload();
    if (upload != null && upload.oss() != null) {
      prefix = trimSlashes(upload.oss().dir());
    }
    String core = trimSlashes(day) + "/" + trimSlashes(name);
    return prefix.isEmpty() ? core : (prefix + "/" + core);
  }

  private static String publicUrl(AppProperties.Oss oss, String bucket, String endpoint, String objectKey) {
    String base = trim(oss.baseUrl());
    if (!base.isEmpty()) {
      return removeTrailingSlash(base) + "/" + objectKey;
    }
    String host = removeScheme(endpoint);
    return "https://" + bucket + "." + host + "/" + objectKey;
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private static String trimSlashes(String s) {
    String x = trim(s);
    while (x.startsWith("/")) x = x.substring(1);
    while (x.endsWith("/")) x = x.substring(0, x.length() - 1);
    return x;
  }

  private static String removeTrailingSlash(String s) {
    String x = trim(s);
    while (x.endsWith("/")) x = x.substring(0, x.length() - 1);
    return x;
  }

  private static String removeScheme(String endpoint) {
    String x = trim(endpoint);
    if (x.startsWith("https://")) return x.substring("https://".length());
    if (x.startsWith("http://")) return x.substring("http://".length());
    return x;
  }
}
