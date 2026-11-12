// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Datas/horas trocadas com o backend: hora local do aparelho, sem fuso (ex.: 2026-11-12T08:00). */
public final class DoseTimes {
    public static final DateTimeFormatter ISO_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    private DoseTimes() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    }

    public static String format(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.MINUTES).format(ISO_MINUTES);
    }

    public static LocalDateTime parse(String value) {
        return LocalDateTime.parse(value);
    }

    public static String display(String isoValue) {
        try {
            return parse(isoValue).format(DISPLAY);
        } catch (RuntimeException exception) {
            return isoValue;
        }
    }

    public static String hour(String isoValue) {
        try {
            return parse(isoValue).format(HOUR);
        } catch (RuntimeException exception) {
            return isoValue;
        }
    }
}
