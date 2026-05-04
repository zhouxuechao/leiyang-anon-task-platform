package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {
  private final EntityManager em;

  public AdminStatsController(EntityManager em) {
    this.em = em;
  }

  public record Overview(
      long usersTotal,
      long tasksTotal,
      long tasksPendingAudit,
      long ordersSubmitted,
      long withdrawPending,
      long reportsPending,
      long todayNewUsers,
      long todayTasksPublished,
      long todayOrdersSettled,
      long todayWithdrawApply
  ) {}

  public record DailyItem(
      String date,
      long newUsers,
      long tasksPublished,
      long ordersAccepted,
      long ordersSubmitted,
      long ordersSettled,
      long withdrawApplied,
      long reportsCreated
  ) {}

  public record UserGrowth(String range, long newUsers) {}

  @GetMapping("/overview")
  public ApiResult<Overview> overview() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    return ApiResult.ok(new Overview(
        count("select count(1) from user_account"),
        count("select count(1) from task_publish"),
        count("select count(1) from task_publish where status = 'PENDING_AUDIT'"),
        count("select count(1) from task_order where order_status = 'SUBMITTED'"),
        count("select count(1) from withdraw_apply where audit_status = 'PENDING'"),
        count("select count(1) from report_record where status = 'PENDING'"),
        count("select count(1) from user_account where cast(created_at as date) = :d", Map.of("d", today)),
        count("select count(1) from task_publish where status = 'PUBLISHED' and cast(created_at as date) = :d", Map.of("d", today)),
        count("select count(1) from task_order where order_status = 'SETTLED' and cast(updated_at as date) = :d", Map.of("d", today)),
        count("select count(1) from withdraw_apply where cast(created_at as date) = :d", Map.of("d", today))
    ));
  }

  @GetMapping("/daily")
  public ApiResult<List<DailyItem>> daily(@RequestParam(defaultValue = "14") int days) {
    int n = Math.min(60, Math.max(7, days));
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate from = today.minusDays(n - 1L);

    Map<LocalDate, long[]> bucket = new LinkedHashMap<>();
    for (int i = 0; i < n; i++) {
      bucket.put(from.plusDays(i), new long[] {0, 0, 0, 0, 0, 0, 0});
    }

    fill(bucket, 0, """
        select cast(created_at as date) as d, count(1) as c
        from user_account
        where cast(created_at as date) >= :from
        group by cast(created_at as date)
        """, Map.of("from", from));

    fill(bucket, 1, """
        select cast(created_at as date) as d, count(1) as c
        from task_publish
        where status = 'PUBLISHED' and cast(created_at as date) >= :from
        group by cast(created_at as date)
        """, Map.of("from", from));

    fill(bucket, 2, """
        select cast(accept_time as date) as d, count(1) as c
        from task_order
        where order_status in ('ACCEPTED','SUBMITTED','APPROVED','SETTLED','REJECTED_RESUBMIT','REJECTED_CLOSE','TIMEOUT')
          and cast(accept_time as date) >= :from
        group by cast(accept_time as date)
        """, Map.of("from", from));

    fill(bucket, 3, """
        select cast(submit_time as date) as d, count(1) as c
        from task_order
        where submit_time is not null and cast(submit_time as date) >= :from
        group by cast(submit_time as date)
        """, Map.of("from", from));

    fill(bucket, 4, """
        select cast(settled_time as date) as d, count(1) as c
        from task_order
        where settled_time is not null and cast(settled_time as date) >= :from
        group by cast(settled_time as date)
        """, Map.of("from", from));

    fill(bucket, 5, """
        select cast(created_at as date) as d, count(1) as c
        from withdraw_apply
        where cast(created_at as date) >= :from
        group by cast(created_at as date)
        """, Map.of("from", from));

    fill(bucket, 6, """
        select cast(created_at as date) as d, count(1) as c
        from report_record
        where cast(created_at as date) >= :from
        group by cast(created_at as date)
        """, Map.of("from", from));

    List<DailyItem> out = new ArrayList<>();
    for (var e : bucket.entrySet()) {
      LocalDate d = e.getKey();
      long[] a = e.getValue();
      out.add(new DailyItem(d.toString(), a[0], a[1], a[2], a[3], a[4], a[5], a[6]));
    }
    return ApiResult.ok(out);
  }

  @GetMapping("/user-growth")
  public ApiResult<List<UserGrowth>> userGrowth() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    return ApiResult.ok(List.of(
        new UserGrowth("7天", countUsersSince(today.minusDays(6))),
        new UserGrowth("15天", countUsersSince(today.minusDays(14))),
        new UserGrowth("1个月", countUsersSince(today.minusMonths(1).plusDays(1)))
    ));
  }

  private long countUsersSince(LocalDate from) {
    return count("select count(1) from user_account where cast(created_at as date) >= :from", Map.of("from", from));
  }

  private long count(String sql) {
    return count(sql, Map.of());
  }

  private long count(String sql, Map<String, Object> params) {
    Query q = em.createNativeQuery(sql);
    for (var e : params.entrySet()) q.setParameter(e.getKey(), e.getValue());
    Object v = q.getSingleResult();
    if (v instanceof Number n) return n.longValue();
    return Long.parseLong(String.valueOf(v));
  }

  private void fill(Map<LocalDate, long[]> bucket, int idx, String sql, Map<String, Object> params) {
    Query q = em.createNativeQuery(sql);
    for (var e : params.entrySet()) q.setParameter(e.getKey(), e.getValue());
    @SuppressWarnings("unchecked")
    List<Object[]> rows = q.getResultList();
    for (Object[] r : rows) {
      if (r == null || r.length < 2 || r[0] == null || r[1] == null) continue;
      LocalDate d = toDate(r[0]);
      if (!bucket.containsKey(d)) continue;
      long c = ((Number) r[1]).longValue();
      bucket.get(d)[idx] = c;
    }
  }

  private static LocalDate toDate(Object v) {
    if (v instanceof java.sql.Date d) return d.toLocalDate();
    if (v instanceof java.time.LocalDate d) return d;
    return LocalDate.parse(v.toString());
  }
}
