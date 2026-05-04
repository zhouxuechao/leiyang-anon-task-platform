package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaPost;
import com.leiyang.anontask.domain.PlazaPostComment;
import com.leiyang.anontask.domain.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlazaPostCommentRepository extends JpaRepository<PlazaPostComment, Long> {
  List<PlazaPostComment> findByPostOrderByCreatedAtAsc(PlazaPost post);

  long countByPost(PlazaPost post);

  boolean existsByPostAndUser(PlazaPost post, UserAccount user);

  void deleteByPost(PlazaPost post);
}
