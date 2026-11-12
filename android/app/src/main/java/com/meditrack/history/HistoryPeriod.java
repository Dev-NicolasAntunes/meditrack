// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * Período exibido no histórico e nas estatísticas: o tipo (dia, semana ou mês) e uma data âncora
 * dentro dele. Os limites seguem as mesmas regras do backend (semana de segunda a domingo).
 */
public final class HistoryPeriod {
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private final String type;
    private final LocalDate anchor;

    private HistoryPeriod(String type, LocalDate anchor) {
        this.type = type;
        this.anchor = anchor;
    }

    /** Período atual (o que contém hoje). */
    public static HistoryPeriod current(String type) {
        return new HistoryPeriod(type, today());
    }

    /** Período do tipo informado que contém a data ISO (AAAA-MM-DD); sem data válida, usa hoje. */
    public static HistoryPeriod of(String type, String isoAnchor) {
        LocalDate anchor;
        try {
            anchor = isoAnchor == null ? today() : LocalDate.parse(isoAnchor);
        } catch (RuntimeException exception) {
            anchor = today();
        }
        return new HistoryPeriod(type, anchor.isAfter(today()) ? today() : anchor);
    }

    public String getType() {
        return type;
    }

    /** Âncora em ISO, para passar entre telas. */
    public String getAnchor() {
        return anchor.toString();
    }

    /** Mesmo dia âncora com outro tipo de período (ex.: da semana para o mês que contém a âncora). */
    public HistoryPeriod withType(String newType) {
        return new HistoryPeriod(newType, anchor);
    }

    public HistoryPeriod previous() {
        return new HistoryPeriod(type, shift(-1));
    }

    /** Próximo período, sem passar do atual. */
    public HistoryPeriod next() {
        if (!hasNext()) {
            return this;
        }
        LocalDate shifted = shift(1);
        return new HistoryPeriod(type, shifted.isAfter(today()) ? today() : shifted);
    }

    public boolean hasNext() {
        return end().isBefore(today());
    }

    public LocalDate start() {
        switch (type) {
            case HistoryRepository.PERIOD_WEEK:
                return anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case HistoryRepository.PERIOD_MONTH:
                return anchor.withDayOfMonth(1);
            default:
                return anchor;
        }
    }

    public LocalDate end() {
        switch (type) {
            case HistoryRepository.PERIOD_WEEK:
                return start().plusDays(6);
            case HistoryRepository.PERIOD_MONTH:
                return anchor.with(TemporalAdjusters.lastDayOfMonth());
            default:
                return anchor;
        }
    }

    /** Intervalo para exibir, ex.: "09/11 – 15/11" ou "12/11". */
    public String label() {
        LocalDate start = start();
        LocalDate end = end();
        return start.equals(end) ? start.format(LABEL) : start.format(LABEL) + " – " + end.format(LABEL);
    }

    /**
     * Parâmetro reference do backend. O backend também usa essa data/hora como "agora" para contar as
     * doses previstas; por isso, no período atual vai a hora do aparelho e, em períodos passados,
     * o último minuto do período (assim todas as doses dele contam).
     */
    public String reference() {
        return hasNext() ? DoseTimes.format(end().atTime(23, 59)) : DoseTimes.format(DoseTimes.now());
    }

    private LocalDate shift(int amount) {
        switch (type) {
            case HistoryRepository.PERIOD_WEEK:
                return anchor.plusWeeks(amount);
            case HistoryRepository.PERIOD_MONTH:
                return anchor.plusMonths(amount);
            default:
                return anchor.plusDays(amount);
        }
    }

    private static LocalDate today() {
        return DoseTimes.now().toLocalDate();
    }
}
