package com.hrflow.storage.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration MinIO.
 *
 * @EnableConfigurationProperties enregistre MinioProperties comme bean Spring
 * et déclenche la validation @NotBlank au démarrage — sans avoir besoin
 * de @Component sur MinioProperties elle-même.
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}
