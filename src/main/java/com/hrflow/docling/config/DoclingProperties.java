package com.hrflow.docling.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriétés de connexion à docling-serve, lues depuis application.yaml.
 *
 * <pre>
 * app:
 *   docling:
 *     base-url: ${DOCLING_URL:http://localhost:5001}
 *     timeout-seconds: 120
 * </pre>
 *
 *