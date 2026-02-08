package com.example.sejong_voice.exception;

public interface ErrorCode {
  int getStatus();
  String getCode();
  String getMessage();
}
