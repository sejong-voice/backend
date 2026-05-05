package com.example.sejong_voice.user;

import com.example.sejong_voice.auth.exception.AuthErrorCode;
import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;

  public User getById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
  }
}
