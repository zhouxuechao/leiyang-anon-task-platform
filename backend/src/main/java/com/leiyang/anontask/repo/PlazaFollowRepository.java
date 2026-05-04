package com.leiyang.anontask.repo;

import com.leiyang.anontask.domain.PlazaFollow;
import com.leiyang.anontask.domain.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlazaFollowRepository extends JpaRepository<PlazaFollow, Long> {
  Optional<PlazaFollow> findByUserAndTargetUser(UserAccount user, UserAccount targetUser);
  List<PlazaFollow> findByUser(UserAccount user);
  List<PlazaFollow> findByUser_Id(Long userId);
  List<PlazaFollow> findByUser_IdAndTargetUser_IdIn(Long userId, Collection<Long> targetUserIds);
  @Query("select f from PlazaFollow f where f.user = :user")
  List<PlazaFollow> findFollowingItems(@Param("user") UserAccount user, Pageable pageable);
  @Query("select f from PlazaFollow f where f.targetUser = :targetUser")
  List<PlazaFollow> findFollowerItems(@Param("targetUser") UserAccount targetUser, Pageable pageable);
  long countByUser(UserAccount user);
  long countByTargetUser(UserAccount targetUser);
}
