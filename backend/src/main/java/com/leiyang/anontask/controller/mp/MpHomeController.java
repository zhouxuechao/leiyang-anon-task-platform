package com.leiyang.anontask.controller.mp;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.repo.SysBannerRepository;
import com.leiyang.anontask.repo.SysNoticeRepository;
import com.leiyang.anontask.service.ConfigService;
import com.leiyang.anontask.service.RedisSupportService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mp/home")
public class MpHomeController {
  private final SysBannerRepository bannerRepo;
  private final SysNoticeRepository noticeRepo;
  private final ConfigService configService;
  private final RedisSupportService redisSupport;

  public MpHomeController(SysBannerRepository bannerRepo, SysNoticeRepository noticeRepo, ConfigService configService, RedisSupportService redisSupport) {
    this.bannerRepo = bannerRepo;
    this.noticeRepo = noticeRepo;
    this.configService = configService;
    this.redisSupport = redisSupport;
  }

  public record BannerItem(long id, String imageUrl, String linkUrl) {}
  public record NoticeItem(long id, String title, String content) {}
  public record HomeResp(List<BannerItem> banners, List<NoticeItem> notices, List<String> navHotTabs) {}

  @GetMapping("")
  public ApiResult<HomeResp> home() {
    String cacheKey = "cache:mp:home:v1";
    HomeResp cached = redisSupport.getJson(cacheKey, HomeResp.class);
    if (cached != null) return ApiResult.ok(cached);
    var banners = bannerRepo.findTop10ByStatusOrderBySortNoAscCreatedAtDesc("ACTIVE").stream()
        .map(b -> new BannerItem(b.getId(), b.getImageUrl(), b.getLinkUrl()))
        .toList();
    var notices = noticeRepo.findTop20ByStatusOrderBySortNoAscCreatedAtDesc("ACTIVE").stream()
        .map(n -> new NoticeItem(n.getId(), n.getTitle(), n.getContent()))
        .toList();
    var hotTabs = Arrays.stream(configService.getString("ui.nav_hot_tabs", "plaza,tasks,music").split(","))
        .map(String::trim)
        .filter(v -> !v.isEmpty())
        .map(String::toLowerCase)
        .distinct()
        .toList();
    HomeResp resp = new HomeResp(banners, notices, hotTabs);
    redisSupport.setJson(cacheKey, resp, Duration.ofSeconds(30));
    return ApiResult.ok(resp);
  }
}
