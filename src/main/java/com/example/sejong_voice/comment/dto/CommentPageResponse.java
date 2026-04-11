package com.example.sejong_voice.comment.dto;

import java.util.List;

public record CommentPageResponse(
    List<CommentResponse> content,
    int page,
    int size,
    int numberOfElements,
    long displayableRootCommentCount,
    long activeCommentCount,
    int totalPages,
    boolean first,
    boolean last
) {

  public static CommentPageResponse of(
      List<CommentResponse> content,
      int page,
      int size,
      long displayableRootCommentCount,
      long activeCommentCount
  ) {
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) displayableRootCommentCount / size);

    return new CommentPageResponse(
        content,
        page,
        size,
        content.size(),
        displayableRootCommentCount,
        activeCommentCount,
        totalPages,
        page == 0,
        page + 1 >= totalPages
    );
  }
}
