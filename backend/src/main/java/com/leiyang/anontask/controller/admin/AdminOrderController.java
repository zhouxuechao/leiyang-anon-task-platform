package com.leiyang.anontask.controller.admin;

import com.leiyang.anontask.common.ApiResult;
import com.leiyang.anontask.common.BizException;
import com.leiyang.anontask.domain.enums.OrderStatus;
import com.leiyang.anontask.dto.admin.AuditRequest;
import com.leiyang.anontask.dto.admin.OrderDetailResponse;
import com.leiyang.anontask.dto.admin.PageResponse;
import com.leiyang.anontask.repo.TaskOrderRepository;
import com.leiyang.anontask.repo.TaskSubmitProofRepository;
import com.leiyang.anontask.service.AdminAuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
  private final TaskOrderRepository orderRepo;
  private final TaskSubmitProofRepository proofRepo;
  private final AdminAuditService auditService;

  public AdminOrderController(
      TaskOrderRepository orderRepo,
      TaskSubmitProofRepository proofRepo,
      AdminAuditService auditService
  ) {
    this.orderRepo = orderRepo;
    this.proofRepo = proofRepo;
    this.auditService = auditService;
  }

  public record SubmittedOrderItem(String orderNo, String taskNo, String taskTitle, String userOpenId, String submitTime) {}

  @GetMapping("/submitted")
  public ApiResult<PageResponse<SubmittedOrderItem>> submitted(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(size, 1), 100);
    var p = orderRepo.findByOrderStatus(OrderStatus.SUBMITTED, PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    var list = p.getContent().stream()
        .map(o -> new SubmittedOrderItem(
            o.getOrderNo(),
            o.getTask().getTaskNo(),
            o.getTask().getTitle(),
            o.getAcceptUser().getOpenId(),
            o.getSubmitTime() == null ? null : o.getSubmitTime().toString()
        ))
        .toList();
    return ApiResult.ok(new PageResponse<>(safePage, safeSize, p.getTotalElements(), list));
  }

  @GetMapping("/{orderNo}")
  public ApiResult<OrderDetailResponse> detail(@PathVariable String orderNo) {
    var o = orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new BizException("Order not found"));
    var proofs = proofRepo.findByOrderOrderByIdAsc(o).stream()
        .map(p -> new OrderDetailResponse.ProofItem(
            p.getProofType(),
            p.getProofUrl(),
            p.getRemark(),
            p.getCreatedAt().toString()
        ))
        .toList();
    var resp = new OrderDetailResponse(
        o.getOrderNo(),
        o.getOrderStatus().name(),
        o.getAuditReason(),
        o.getAcceptUser().getOpenId(),
        o.getSubmitTime() == null ? null : o.getSubmitTime().toString(),
        o.getTask().getTaskNo(),
        o.getTask().getTitle(),
        o.getTask().getAmount(),
        proofs
    );
    return ApiResult.ok(resp);
  }

  @PostMapping("/{orderNo}/audit")
  public ApiResult<Void> audit(@PathVariable String orderNo, @Valid @RequestBody AuditRequest req) {
    auditService.auditOrder(orderNo, req.result(), req.reason());
    return ApiResult.ok(null);
  }
}
