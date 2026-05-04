package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaPost;
import com.leiyang.anontask.domain.PlazaPostImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlazaPostImageRepository extends JpaRepository<PlazaPostImage, Long> {
  List<PlazaPostImage> findByPostOrderBySortNoAsc(PlazaPost post);

  @Query("select i from PlazaPostImage i where i.post in :posts order by i.post.id asc, i.sortNo asc")
  List<PlazaPostImage> findByPostInOrderByPostIdAscSortNoAsc(Collection<PlazaPost> posts);

  void deleteByPost(PlazaPost post);
}
