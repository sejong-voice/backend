package com.example.sejong_voice.image.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.oci.object-storage")
public class OciObjectStorageProperties {

  private static final String DEFAULT_IMAGE_PREFIX = "post-images";
  private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
  private static final long DEFAULT_TEMP_IMAGE_RETENTION_HOURS = 24L;

  @NotBlank
  private final String region;

  @NotBlank
  private final String bucket;

  @NotBlank
  private final String namespace;

  public String getImagePrefix() {
    return DEFAULT_IMAGE_PREFIX;
  }

  public long getMaxFileSizeBytes() {
    return DEFAULT_MAX_FILE_SIZE_BYTES;
  }

  public long getTempImageRetentionHours() {
    return DEFAULT_TEMP_IMAGE_RETENTION_HOURS;
  }
}
