package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.AiMusicJob;
import com.leiyang.anontask.domain.AiMusicLike;
import com.leiyang.anontask.domain.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMusicLikeRepository extends JpaRepository<AiMusicLike, Long> {
  Optional<AiMusicLike> findByMusicAndUser(AiMusicJob music, UserAccount user);

  List<AiMusicLike> findByUserAndMusicIn(UserAccount user, Collection<AiMusicJob> music);

  void deleteByMusic(AiMusicJob music);
}
