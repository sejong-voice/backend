package com.example.sejong_voice.comment.repository;

import com.example.sejong_voice.user.entity.UserRole;
import java.util.UUID;

public record CommentAuthorProjection(UUID userId, UserRole role) {}
