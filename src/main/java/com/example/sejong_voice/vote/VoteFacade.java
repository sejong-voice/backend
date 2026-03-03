package com.example.sejong_voice.vote;

import com.example.sejong_voice.post.entity.Post;
import com.example.sejong_voice.post.PostService;
import com.example.sejong_voice.user.entity.User;
import com.example.sejong_voice.user.UserService;
import com.example.sejong_voice.vote.dto.VoteSummaryResponse;
import com.example.sejong_voice.vote.entity.VoteChoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VoteFacade {

  private final VoteService voteService;
  private final UserService userService;
  private final PostService postService;

  @Transactional
  public void cast(UUID postId, UUID userId, VoteChoice choice) {
    User user = userService.getById(userId);
    Post post = postService.getById(postId);
    
    voteService.cast(post, user, choice);
  }

  @Transactional(readOnly = true)
  public VoteSummaryResponse getSummary(UUID postId) {
    return voteService.getSummary(postId);
  }
}
