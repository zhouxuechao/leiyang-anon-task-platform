package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.AiMusicComment;
import com.leiyang.anontask.domain.AiMusicJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMusicCommentRepository extends JpaRepository<AiMusicComment, Long> {
  List<AiMusicComment> findTop50ByMusicOrderByCreatedAtDesc(AiMusicJob music);

  void deleteByMusic(AiMusicJob music);
}
