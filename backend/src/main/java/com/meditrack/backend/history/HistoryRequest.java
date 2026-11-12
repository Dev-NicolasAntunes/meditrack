// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

/**
 * Datas e horários chegam como data/hora local do aparelho (ex.: 2026-11-12T08:00), sem fuso.
 */
public record HistoryRequest(
        String medicationId,
        String scheduledAt,
        String actionAt,
        String status
) {
}
