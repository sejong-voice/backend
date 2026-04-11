package com.example.sejong_voice.comment;

import com.example.sejong_voice.comment.dto.CommentResponse;
import com.example.sejong_voice.comment.dto.ReplyResponse;
import com.example.sejong_voice.comment.entity.Comment;
import com.example.sejong_voice.comment.entity.CommentStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CommentResponseAssembler {

  public List<CommentResponse> assemble(
      List<Comment> rootComments,
      List<Comment> replies,
      UUID currentUserId,
      UUID postAuthorId,
      Map<UUID, Integer> anonymousNumberByUserId
  ) {
    Map<UUID, List<Comment>> activeRepliesByRootId = createActiveRepliesByRootId(replies);

    return rootComments.stream()
        .map(comment -> CommentResponse.from(
            comment,
            currentUserId,
            postAuthorId,
            anonymousNumberByUserId,
            toReplyResponses(
                activeRepliesByRootId.getOrDefault(comment.getId(), List.of()),
                currentUserId,
                postAuthorId,
                anonymousNumberByUserId
            )
        ))
        .toList();
  }

  private Map<UUID, List<Comment>> createActiveRepliesByRootId(List<Comment> comments) {
    Map<UUID, List<Comment>> activeRepliesByRootId = new LinkedHashMap<>();

    for (Comment comment : comments) {
      Comment rootComment = comment.getRootComment();

      if (rootComment == null || comment.getStatus() != CommentStatus.ACTIVE) {
        continue;
      }

      activeRepliesByRootId.computeIfAbsent(rootComment.getId(), ignored -> new ArrayList<>())
          .add(comment);
    }

    return activeRepliesByRootId;
  }

  private List<ReplyResponse> toReplyResponses(
      List<Comment> comments,
      UUID currentUserId,
      UUID postAuthorId,
      Map<UUID, Integer> anonymousNumberByUserId
  ) {
    return comments.stream()
        .map(comment -> ReplyResponse.from(
            comment,
            currentUserId,
            postAuthorId,
            anonymousNumberByUserId
        ))
        .toList();
  }
}
