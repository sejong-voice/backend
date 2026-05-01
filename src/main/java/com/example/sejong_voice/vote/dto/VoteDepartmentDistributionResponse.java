package com.example.sejong_voice.vote.dto;

public record VoteDepartmentDistributionResponse(
    String department, long agreeCount, long disagreeCount, long totalCount) {}
