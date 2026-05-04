package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.RechargeOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechargeOrderRepository extends JpaRepository<RechargeOrder, Long> {
  Optional<RechargeOrder> findByOutTradeNo(String outTradeNo);
}
