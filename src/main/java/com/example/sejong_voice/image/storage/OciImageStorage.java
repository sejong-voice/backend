package com.example.sejong_voice.image.storage;

import com.example.sejong_voice.image.config.OciObjectStorageProperties;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import java.io.IOException;
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

  public String getPublicUrl(String objectName) {
    return "%s/%s".formatted(resolveObjectBaseUrl(), objectName);
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
    return "%s/%s/%s".formatted(
        properties.getImagePrefix(),
        userId,
        UUID.randomUUID()
    );
  }

  private String resolveObjectBaseUrl() {
    return "https://%s.objectstorage.%s.oci.customer-oci.com/n/%s/b/%s/o".formatted(
        properties.getNamespace(),
        properties.getRegion(),
        properties.getNamespace(),
        properties.getBucket()
    );
  }
}
