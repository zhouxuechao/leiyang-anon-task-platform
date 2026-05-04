package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.AiTaskDraft;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTaskDraftRepository extends JpaRepository<AiTaskDraft, Long> {
  List<AiTaskDraft> findTop200ByOrderByCreatedAtDesc();
  Optional<AiTaskDraft> findTop1ByStatusOrderByUpdatedAtDesc(String status);
  List<AiTaskDraft> findByStatusOrderByUpdatedAtDesc(String status);
}
