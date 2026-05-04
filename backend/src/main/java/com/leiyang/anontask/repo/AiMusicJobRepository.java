package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.AiMusicJob;
import com.leiyang.anontask.domain.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMusicJobRepository extends JpaRepository<AiMusicJob, Long> {
  @Override
  @EntityGraph(attributePaths = "user")
  Optional<AiMusicJob> findById(Long id);

  @EntityGraph(attributePaths = "user")
  List<AiMusicJob> findByUserOrderByCreatedAtDesc(UserAccount user, Pageable pageable);

  long countByUser(UserAccount user);

  long countByUserAndCreatedAtGreaterThanEqual(UserAccount user, Instant createdAt);

  @EntityGraph(attributePaths = "user")
  List<AiMusicJob> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  List<AiMusicJob> findByPublishedTrueOrderByRatingTotalDescTipTotalDescPublishedAtDesc(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  List<AiMusicJob> findByUserIdAndPublishedTrueOrderByPublishedAtDesc(Long userId, Pageable pageable);

  @EntityGraph(attributePaths = "user")
  Optional<AiMusicJob> findBySunoTaskId(String sunoTaskId);

  @EntityGraph(attributePaths = "user")
  Optional<AiMusicJob> findByPlazaPostId(Long plazaPostId);

  @EntityGraph(attributePaths = "user")
  List<AiMusicJob> findAllByPlazaPostId(Long plazaPostId);

  @EntityGraph(attributePaths = "user")
  List<AiMusicJob> findByPlazaPostIdIn(Collection<Long> plazaPostIds);

  @EntityGraph(attributePaths = "user")
  List<AiMusicJob> findByStatusInAndSunoTaskIdIsNotNullOrderByUpdatedAtAsc(List<String> statuses, Pageable pageable);
}
