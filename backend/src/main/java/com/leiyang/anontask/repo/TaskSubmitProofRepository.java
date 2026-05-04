package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.TaskOrder;
import com.leiyang.anontask.domain.TaskSubmitProof;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskSubmitProofRepository extends JpaRepository<TaskSubmitProof, Long> {
  List<TaskSubmitProof> findByOrderOrderByIdAsc(TaskOrder order);
  void deleteByOrder(TaskOrder order);
}

