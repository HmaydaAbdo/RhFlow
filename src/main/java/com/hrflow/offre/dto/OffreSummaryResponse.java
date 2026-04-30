package com.hrflow.offre.dto;

import java.time.LocalDateTime;

public record OffreSummaryResponse(
    Long           id,
    LocalDateTime  generatedAt,
    LocalDateTime  updatedAt
) {}
