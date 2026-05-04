package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.TaskComment;
import com.leiyang.anontask.domain.TaskPublish;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
  List<TaskComment> findTop50ByTaskOrderByCreatedAtDesc(TaskPublish task);
}

