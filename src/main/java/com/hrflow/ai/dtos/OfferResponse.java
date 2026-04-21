package com.hrflow.ai.dtos;

/**
 * Response body for the offer generation endpoint.
 * The {@code content} field contains Markdown-formatted text
 * ready to be rendered by the frontend.
 */
public record OfferResponse(String content) {}
