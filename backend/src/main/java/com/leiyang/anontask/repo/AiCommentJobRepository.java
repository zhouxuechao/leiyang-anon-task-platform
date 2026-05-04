package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.AiCommentJob;
import com.leiyang.anontask.domain.PlazaPost;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiCommentJobRepository extends JpaRepository<AiCommentJob, Long> {
  Optional<AiCommentJob> findByPostIdAndProviderCode(Long postId, String providerCode);

  void deleteByPost(PlazaPost post);

  List<AiCommentJob> findTop100ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(String status, Instant now);

  @Query("""
      select j from AiCommentJob j
      where (:status = '' or upper(j.status) = upper(:status))
        and (
          :q = ''
          or lower(j.providerCode) like lower(concat('%', :q, '%'))
          or str(j.post.id) like concat('%', :q, '%')
          or lower(coalesce(j.lastError, '')) like lower(concat('%', :q, '%'))
        )
      """)
  Page<AiCommentJob> adminSearch(@Param("status") String status, @Param("q") String q, Pageable pageable);

  long countByStatus(String status);
}
