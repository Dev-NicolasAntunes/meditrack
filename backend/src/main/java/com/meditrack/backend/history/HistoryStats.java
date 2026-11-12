// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

public record HistoryStats(
        String period,
        String startDate,
        String endDate,
        int expectedDoses,
        int takenDoses,
        int adherencePercent,
        int tomado,
        int atrasado,
        int adiado,
        int naoTomado
) {
}
