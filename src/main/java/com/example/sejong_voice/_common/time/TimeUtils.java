package com.example.sejong_voice._common.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class TimeUtils {

  private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");
  private static final ZoneId UTC = ZoneOffset.UTC;

  private TimeUtils() {}

  public static LocalDateTime toSeoulLocalDateTime(Instant instant) {
    return instant.atZone(ASIA_SEOUL).toLocalDateTime();
  }

  public static Instant toUtcInstant(LocalDateTime seoulDateTime) {
    return seoulDateTime.atZone(ASIA_SEOUL).withZoneSameInstant(UTC).toInstant();
  }
}
