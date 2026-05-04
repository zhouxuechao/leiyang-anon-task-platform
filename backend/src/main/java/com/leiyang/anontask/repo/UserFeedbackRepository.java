package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.UserFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
  @Query("""
      select f from UserFeedback f
      where (
        :q = ''
        or lower(coalesce(f.content, '')) like lower(concat('%', :q, '%'))
        or lower(coalesce(f.contact, '')) like lower(concat('%', :q, '%'))
        or lower(coalesce(f.user.nickname, '')) like lower(concat('%', :q, '%'))
        or lower(coalesce(f.user.openId, '')) like lower(concat('%', :q, '%'))
        or str(f.user.id) like concat('%', :q, '%')
      )
      """)
  Page<UserFeedback> adminSearch(@Param("q") String q, Pageable pageable);
}
