package com.example.sejong_voice.image.storage;

import com.example.sejong_voice.image.config.OciObjectStorageProperties;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class OciImageStorage {

  private final ObjectProvider<ObjectStorageClient> objectStorageClientProvider;
  private final OciObjectStorageProperties properties;

  public String upload(UUID userId, MultipartFile file, String contentType) throws IOException {
    String objectName = buildObjectName(userId, file.getOriginalFilename());

    PutObjectRequest request = PutObjectRequest.builder()
        .namespaceName(properties.getNamespace())
        .bucketName(properties.getBucket())
        .objectName(objectName)
        .contentType(contentType)
        .contentLength(file.getSize())
        .putObjectBody(file.getInputStream())
        .build();

    getClient().putObject(request);
    return objectName;
  }

  public String createReadAccessUrl(String objectName) {
    CreatePreauthenticatedRequestDetails details = CreatePreauthenticatedRequestDetails.builder()
        .name("post-image-read-" + UUID.randomUUID())
        .objectName(objectName)
        .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
        .timeExpires(Date.from(
            Instant.now().plusSeconds(properties.getAccessUrlExpirationMinutes() * 60)
        ))
        .build();

    CreatePreauthenticatedRequestRequest request = CreatePreauthenticatedRequestRequest.builder()
        .namespaceName(properties.getNamespace())
        .bucketName(properties.getBucket())
        .createPreauthenticatedRequestDetails(details)
        .build();

    String accessUri = getClient().createPreauthenticatedRequest(request)
        .getPreauthenticatedRequest()
        .getAccessUri();

    return resolvePublicUrl(accessUri);
  }

  public void delete(String objectName) {
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .namespaceName(properties.getNamespace())
        .bucketName(properties.getBucket())
        .objectName(objectName)
        .build();

    getClient().deleteObject(request);
  }

  private ObjectStorageClient getClient() {
    return objectStorageClientProvider.getObject();
  }

  private String buildObjectName(UUID userId, String originalFileName) {
    String extension = extractExtension(originalFileName);
    return "%s/temp/%s/%s%s".formatted(
        properties.getImagePrefix(),
        userId,
        UUID.randomUUID(),
        extension
    );
  }

  private String extractExtension(String originalFileName) {
    if (originalFileName == null || !originalFileName.contains(".")) {
      return "";
    }
    return originalFileName.substring(originalFileName.lastIndexOf('.'));
  }

  private String resolvePublicUrl(String accessUri) {
    String endpoint = properties.getEndpoint();
    if (endpoint == null || endpoint.isBlank()) {
      endpoint = "https://objectstorage.%s.oraclecloud.com".formatted(properties.getRegion());
    }

    if (endpoint.endsWith("/")) {
      endpoint = endpoint.substring(0, endpoint.length() - 1);
    }

    return endpoint + accessUri;
  }
}
