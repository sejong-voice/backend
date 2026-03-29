package com.example.sejong_voice.council.repository;

import com.example.sejong_voice.council.entity.Council;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface CouncilRepository extends JpaRepository<Council, UUID> {
  @Query("select c from Council c left join fetch c.parent")
  List<Council> findAllWithParent();

  @Query("""
      select c
      from Council c
      left join fetch c.parent
      where lower(c.name) like lower(concat('%', :keyword, '%'))
      """)
  List<Council> findByNameContainingIgnoreCaseWithParent(@Param("keyword") String keyword);
}
