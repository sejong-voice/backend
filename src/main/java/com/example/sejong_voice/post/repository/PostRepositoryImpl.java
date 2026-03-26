package com.example.sejong_voice.post.repository;

import static com.example.sejong_voice.council.entity.QCouncilMember.councilMember;
import static com.example.sejong_voice.post.entity.QPost.post;

import com.example.sejong_voice.post.dto.PostListResponse;
import com.example.sejong_voice.post.dto.PostSearchCondition;
import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.entity.PostModerationStatus;
import com.example.sejong_voice.post.entity.PostStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<PostListResponse> search(PostSearchCondition condition, Pageable pageable) {

    // 1. 컨텐츠 조회
    List<Post> content = queryFactory
        .selectFrom(post)
        .join(post.user).fetchJoin()
        .join(post.council).fetchJoin()
        .where(
            statusEq(condition.status()),
            responsibleCouncilEq(condition.councilId()),
            assignedToMe(condition.assignedToMe(), condition.currentUserId()),
            keywordContains(condition.keyword()),
            isMine(condition.mine(), condition.currentUserId()),
            post.visibilityStatus.eq(PostModerationStatus.ACTIVE), // ACTIVE만 조회가능하게
            post.deletedAt.isNull()
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(post.createdAt.desc())
        .fetch();

    // 2. 카운트 쿼리
    Long total = queryFactory
        .select(post.count())
        .from(post)
        .where(
            statusEq(condition.status()),
            responsibleCouncilEq(condition.councilId()),
            assignedToMe(condition.assignedToMe(), condition.currentUserId()),
            keywordContains(condition.keyword()),
            isMine(condition.mine(), condition.currentUserId()),
            post.deletedAt.isNull()
        )
        .fetchOne();

    // 3. 변환 및 반환
    List<PostListResponse> dtoList = content.stream()
        .map(PostListResponse::from)
        .toList();

    return new PageImpl<>(dtoList, pageable, total != null ? total : 0L);
  }


  private BooleanExpression statusEq(PostStatus status) {
    return status != null ? post.status.eq(status) : null;
  }

  private BooleanExpression responsibleCouncilEq(UUID councilId) {
    return councilId != null ? post.council.id.eq(councilId) : null;
  }

  private BooleanExpression assignedToMe(Boolean assignedToMe, UUID currentUserId) {
    if (!Boolean.TRUE.equals(assignedToMe)) {
      return null;
    }

    return queryFactory
        .selectOne()
        .from(councilMember)
        .where(
            councilMember.user.id.eq(currentUserId),
            councilMember.council.id.eq(post.council.id)
        )
        .exists();
  }

  private BooleanExpression keywordContains(String keyword) {
    return StringUtils.hasText(keyword) ?
        post.title.contains(keyword).or(post.content.contains(keyword)) : null;
  }

  private BooleanExpression isMine(Boolean mine, UUID currentUserId) {
    if (Boolean.TRUE.equals(mine)) {
      if (currentUserId == null) {
        return post.id.isNull();
      }
      return post.user.id.eq(currentUserId);
    }
    return null;
  }
}