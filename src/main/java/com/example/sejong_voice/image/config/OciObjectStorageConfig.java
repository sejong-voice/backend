package com.example.sejong_voice.image.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@RequiredArgsConstructor
public class OciObjectStorageConfig {

  private final OciObjectStorageProperties properties;

  @Bean
  @Lazy
  public BasicAuthenticationDetailsProvider ociAuthenticationDetailsProvider() {
    return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
  }

  @Bean
  @Lazy
  public ObjectStorageClient objectStorageClient(
      BasicAuthenticationDetailsProvider authenticationDetailsProvider) {
    ObjectStorageClient client = ObjectStorageClient.builder().build(authenticationDetailsProvider);
    client.setRegion(Region.fromRegionId(properties.getRegion()));
    return client;
  }
}
