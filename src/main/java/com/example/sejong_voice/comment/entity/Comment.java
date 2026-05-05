package com.example.sejong_voice.comment.entity;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comment")
public class Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "root_id")
  private Comment rootComment;

  @OneToMany(mappedBy = "rootComment", cascade = CascadeType.ALL)
  @OrderBy("createdAt ASC")
  private List<Comment> replies = new ArrayList<>();

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CommentStatus status;

  private Instant deletedAt;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Builder
  public Comment(Post post, User user, Comment rootComment, String content, Instant createdAt) {
    this.post = post;
    this.user = user;
    this.rootComment = rootComment;
    this.content = content;
    this.status = CommentStatus.ACTIVE;
    this.createdAt = createdAt;
  }

  public void delete(Instant deletedAt) {
    this.status = CommentStatus.DELETED;
    this.deletedAt = deletedAt;
  }

  public void blind() {
    this.status = CommentStatus.BLINDED;
  }

  public void activate() {
    this.status = CommentStatus.ACTIVE;
  }
}
