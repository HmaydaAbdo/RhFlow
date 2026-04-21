package com.hrflow.besoinrecrutement.dto;

import java.util.List;

public record BesoinStatsResponse(
    long total,
    long enCours,
    long acceptes,
    long refuses,
    List<StatParDirection> parDirection,
    List<StatParPriorite> parPriorite
) {
    public record StatParDirection(Long directionId, String directionNom, long total, long enCours, long acceptes, long refuses) {}
    public record StatParPriorite(String priorite, long total) {}
}
