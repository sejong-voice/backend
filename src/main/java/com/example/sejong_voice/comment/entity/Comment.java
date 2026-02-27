package com.example.sejong_voice.comment.entity;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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
  @JoinColumn(name = "parent_id")
  private Comment parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
  private List<Comment> children = new ArrayList<>();

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CommentStatus status;

  private LocalDateTime deletedAt;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  public Comment(Post post, User user, Comment parent, String content) {
    this.post = post;
    this.user = user;
    this.parent = parent;
    this.content = content;
    this.status = CommentStatus.ACTIVE;
    this.createdAt = LocalDateTime.now();
  }

  public void delete() {
    this.status = CommentStatus.DELETED;
    this.deletedAt = LocalDateTime.now();
  }

  public void blind() {
    this.status = CommentStatus.BLINDED;
  }

  public void activate() {this.status = CommentStatus.ACTIVE;
  }
}
