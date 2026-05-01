package com.example.sejong_voice.council.repository;

import com.example.sejong_voice.council.entity.Council;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouncilRepository extends JpaRepository<Council, UUID> {
  boolean existsByAdminUserId(UUID userId);

  @Query(
      """
      select c
      from Council c
      join fetch c.affiliation a
      left join fetch a.parent
      """)
  List<Council> findAllWithParent();

  @Query(
      """
      select c
      from Council c
      join fetch c.affiliation a
      left join fetch a.parent
      where lower(a.name) like lower(concat('%', :keyword, '%'))
      """)
  List<Council> findByNameContainingIgnoreCaseWithParent(@Param("keyword") String keyword);
}
