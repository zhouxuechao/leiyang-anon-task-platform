package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaPost;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlazaPostRepository extends JpaRepository<PlazaPost, Long> {
  Page<PlazaPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
  long countByAuthorId(Long authorId);

  interface AuthorPostCount {
    Long getAuthorId();
    long getPostCount();
  }

  @Query("""
      select p.author.id as authorId, count(p) as postCount
      from PlazaPost p
      where p.author.id in :authorIds
      group by p.author.id
      """)
  List<AuthorPostCount> countByAuthorIds(@Param("authorIds") Collection<Long> authorIds);

  @Query("""
      select p from PlazaPost p
      where (:category = 'ALL' or upper(p.category) = :category)
        and (
          :gender = 'ALL'
          or upper(p.category) = :gender
          or (upper(p.category) not in ('MALE', 'FEMALE') and upper(p.gender) = :gender)
        )
        and (
          :followOnly = false
          or (:viewerId is not null and p.author.id = :viewerId)
          or (:viewerId is not null and exists (
            select 1 from PlazaFollow f
            where f.user.id = :viewerId and f.targetUser.id = p.author.id
          ))
        )
      """)
  Page<PlazaPost> publicSearch(
      @Param("gender") String gender,
      @Param("category") String category,
      @Param("followOnly") boolean followOnly,
      @Param("viewerId") Long viewerId,
      Pageable pageable
  );

  @Query("""
      select p from PlazaPost p
      where (:category = 'ALL' or upper(p.category) = :category)
        and (
          :gender = 'ALL'
          or upper(p.category) = :gender
          or (upper(p.category) not in ('MALE', 'FEMALE') and upper(p.gender) = :gender)
        )
        and (
          :followOnly = false
          or (:viewerId is not null and p.author.id = :viewerId)
          or (:viewerId is not null and exists (
            select 1 from PlazaFollow f
            where f.user.id = :viewerId and f.targetUser.id = p.author.id
          ))
        )
      """)
  List<PlazaPost> publicSearchItems(
      @Param("gender") String gender,
      @Param("category") String category,
      @Param("followOnly") boolean followOnly,
      @Param("viewerId") Long viewerId,
      Pageable pageable
  );

  @Query("select p from PlazaPost p where p.author.id = :authorId")
  List<PlazaPost> findAuthorItems(@Param("authorId") Long authorId, Pageable pageable);

  @Query("""
      select p from PlazaPost p
      where (:category = '' or upper(p.category) = :category)
        and (
          :q = '' or lower(p.content) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.author.nickname, '')) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.author.openId, '')) like lower(concat('%', :q, '%'))
        )
      """)
  Page<PlazaPost> adminSearch(@Param("category") String category, @Param("q") String q, Pageable pageable);
}
