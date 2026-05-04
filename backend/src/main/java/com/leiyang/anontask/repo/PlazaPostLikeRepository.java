package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaPost;
import com.leiyang.anontask.domain.PlazaPostLike;
import com.leiyang.anontask.domain.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlazaPostLikeRepository extends JpaRepository<PlazaPostLike, Long> {
  Optional<PlazaPostLike> findByPostAndUser(PlazaPost post, UserAccount user);

  List<PlazaPostLike> findByUserAndPostIn(UserAccount user, Collection<PlazaPost> posts);

  void deleteByPost(PlazaPost post);
}
