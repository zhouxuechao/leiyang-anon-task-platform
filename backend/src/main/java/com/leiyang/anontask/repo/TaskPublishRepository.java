package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.TaskPublish;
import com.leiyang.anontask.domain.enums.TaskStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

public interface TaskPublishRepository extends JpaRepository<TaskPublish, Long> {
  Optional<TaskPublish> findByTaskNo(String taskNo);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TaskPublish t where t.taskNo = :taskNo")
  Optional<TaskPublish> findByTaskNoForUpdate(@Param("taskNo") String taskNo);

  List<TaskPublish> findTop50ByStatusOrderByCreatedAtDesc(TaskStatus status);

  @Query("""
      select t from TaskPublish t
      where t.status = :status
        and t.deadlineAt > :now
      order by t.createdAt desc
      """)
  List<TaskPublish> findLatestPublished(@Param("status") TaskStatus status, @Param("now") Instant now);

  @Query("""
      select t from TaskPublish t
      where t.status = :status
        and t.deadlineAt > :now
        and (:q is null or lower(t.title) like concat('%', lower(:q), '%')
          or lower(coalesce(t.locationText, '')) like concat('%', lower(:q), '%')
          or lower(t.taskNo) like concat('%', lower(:q), '%'))
      """)
  Page<TaskPublish> findLatestPublishedPage(
      @Param("status") TaskStatus status,
      @Param("now") Instant now,
      @Param("q") String q,
      Pageable pageable
  );

  @Query("""
      select t from TaskPublish t
      where t.status = :status
        and t.deadlineAt > :now
        and (:q is null or lower(t.title) like concat('%', lower(:q), '%')
          or lower(coalesce(t.locationText, '')) like concat('%', lower(:q), '%')
          or lower(t.taskNo) like concat('%', lower(:q), '%'))
      """)
  List<TaskPublish> findLatestPublishedItems(
      @Param("status") TaskStatus status,
      @Param("now") Instant now,
      @Param("q") String q,
      Pageable pageable
  );

  @Query("""
      select t from TaskPublish t
      where t.status in (
          com.leiyang.anontask.domain.enums.TaskStatus.PUBLISHED,
          com.leiyang.anontask.domain.enums.TaskStatus.EXPIRED
        )
        and t.deadlineAt <= :now
        and (:q is null or lower(t.title) like concat('%', lower(:q), '%')
          or lower(coalesce(t.locationText, '')) like concat('%', lower(:q), '%')
          or lower(t.taskNo) like concat('%', lower(:q), '%'))
      """)
  Page<TaskPublish> findEndedPage(
      @Param("now") Instant now,
      @Param("q") String q,
      Pageable pageable
  );

  @Query("""
      select t from TaskPublish t
      where t.status in (
          com.leiyang.anontask.domain.enums.TaskStatus.PUBLISHED,
          com.leiyang.anontask.domain.enums.TaskStatus.EXPIRED
        )
        and t.deadlineAt <= :now
        and (:q is null or lower(t.title) like concat('%', lower(:q), '%')
          or lower(coalesce(t.locationText, '')) like concat('%', lower(:q), '%')
          or lower(t.taskNo) like concat('%', lower(:q), '%'))
      """)
  List<TaskPublish> findEndedItems(
      @Param("now") Instant now,
      @Param("q") String q,
      Pageable pageable
  );

  @Query("""
      select t from TaskPublish t
      where t.status = com.leiyang.anontask.domain.enums.TaskStatus.PUBLISHED
        and t.deadlineAt <= :now
      """)
  List<TaskPublish> findPublishedExpired(@Param("now") Instant now);

  @Query("""
      select t from TaskPublish t
      where (:status is null or t.status = :status)
        and (:q is null or lower(t.title) like concat('%', lower(:q), '%') or lower(t.content) like concat('%', lower(:q), '%'))
      """)
  Page<TaskPublish> adminSearch(
      @Param("status") TaskStatus status,
      @Param("q") String q,
      Pageable pageable
  );

  long countByStatus(TaskStatus status);

  Page<TaskPublish> findByPublisherIdAndStatusOrderByCreatedAtDesc(Long publisherId, TaskStatus status, Pageable pageable);

  @Query("""
      select t from TaskPublish t
      where t.publisher.id = :publisherId and t.status = :status
      """)
  List<TaskPublish> findPublisherItems(
      @Param("publisherId") Long publisherId,
      @Param("status") TaskStatus status,
      Pageable pageable
  );
}
