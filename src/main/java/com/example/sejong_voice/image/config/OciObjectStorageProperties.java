package com.example.sejong_voice.image.config;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
  private static final long DEFAULT_ACCESS_URL_EXPIRATION_MINUTES = 30L;
  private static final long DEFAULT_TEMP_IMAGE_RETENTION_HOURS = 24L;

  @NotNull
  private final AuthMethod authMethod;

  @NotBlank
  private final String region;

  @NotBlank
  private final String bucket;

  @NotBlank
  private final String namespace;

  @NotBlank
  private final String configFilePath;

  @NotBlank
  private final String profile;

  public String getImagePrefix() {
    return DEFAULT_IMAGE_PREFIX;
  }

  public long getMaxFileSizeBytes() {
    return DEFAULT_MAX_FILE_SIZE_BYTES;
  }

  public long getAccessUrlExpirationMinutes() {
    return DEFAULT_ACCESS_URL_EXPIRATION_MINUTES;
  }

  public long getTempImageRetentionHours() {
    return DEFAULT_TEMP_IMAGE_RETENTION_HOURS;
  }

  @Nullable
  public String getEndpoint() {
    return null;
  }

  public enum AuthMethod {
    CONFIG_FILE,
    INSTANCE_PRINCIPAL
  }
}
