package com.hrflow.docling.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration du client HTTP vers docling-serve.
 *
 * Utilise {@link RestClient} (Spring Boot 3.2+), le client HTTP synchrone moderne
 * qui remplace {@code RestTemplate}. Synchrone = cohérent avec le pipeline async
 * qui tourne dans son propre thread (pipelineExecutor) — pas besoin de reactive.
 *
 * Deux timeouts distincts :
 *  - connectTimeout : 5 s — docling-serve doit répondre rapidement à l'ouverture TCP
 *  - readTimeout    : app.docling.timeout-seconds — la conversion peut être longue
 *                     (30–120 s selon la densité du document et la charge du cluster)
 */
@Configuration
@EnableConfigurationProperties(DoclingProperties.class)
public class DoclingConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean("doclingRestClient")
    public RestClient doclingRestClient(DoclingProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
