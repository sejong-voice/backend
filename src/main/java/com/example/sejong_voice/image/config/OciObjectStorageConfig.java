package com.example.sejong_voice.image.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import java.io.IOException;
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
  public BasicAuthenticationDetailsProvider ociAuthenticationDetailsProvider() throws IOException {
    if (properties.getAuthMethod() == OciObjectStorageProperties.AuthMethod.INSTANCE_PRINCIPAL) {
      return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
    }

    return new ConfigFileAuthenticationDetailsProvider(
        properties.getConfigFilePath(),
        properties.getProfile()
    );
  }

  @Bean
  @Lazy
  public ObjectStorageClient objectStorageClient(
      BasicAuthenticationDetailsProvider authenticationDetailsProvider
  ) {
    ObjectStorageClient client = new ObjectStorageClient(authenticationDetailsProvider);

    if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
      client.setEndpoint(properties.getEndpoint());
    } else {
      client.setRegion(Region.fromRegionId(properties.getRegion()));
    }

    return client;
  }
}
