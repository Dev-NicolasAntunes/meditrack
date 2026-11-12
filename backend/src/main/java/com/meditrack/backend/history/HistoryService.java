// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

import com.meditrack.backend.medication.Medication;
import com.meditrack.backend.medication.MedicationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class HistoryService {
    /** Confirmações feitas mais de 30 minutos depois do horário previsto contam como atraso. */
    static final long LATE_AFTER_MINUTES = 30;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");

    private final HistoryRepository repository;
    private final MedicationService medicationService;

    public HistoryService(HistoryRepository repository, MedicationService medicationService) {
        this.repository = repository;
        this.medicationService = medicationService;
    }

    public DoseRecordResult record(HistoryRequest request) {
        if (request == null || isBlank(request.medicationId())) {
            badRequest("Informe o medicamento.");
        }
        LocalDateTime scheduled = parseDateTime(request.scheduledAt(), "O horário previsto");
        LocalDateTime action = parseDateTime(request.actionAt(), "O horário da ação");
        DoseStatus status = parseStatus(request.status());
        if (status == DoseStatus.TOMADO
                && action.isAfter(scheduled.plusMinutes(LATE_AFTER_MINUTES))) {
            status = DoseStatus.ATRASADO;
        }

        String medicationId = request.medicationId().trim();
        Medication medication = medicationService.findById(medicationId);
        String scheduledAt = format(scheduled);

        Optional<MedicationHistory> previous = repository.findByDose(medicationId, scheduledAt);
        if (previous.isPresent() && previous.get().getStatus().isTaken()) {
            return new DoseRecordResult(previous.get(), medication, true);
        }

        MedicationHistory entry = new MedicationHistory(0, medicationId, medication.getName(),
                medication.getDosage(), scheduledAt, format(action), status);
        if (!repository.upsertUnlessTaken(entry)) {
            // Outra requisição confirmou a mesma dose entre a consulta e a gravação.
            return new DoseRecordResult(currentEntry(medicationId, scheduledAt), medication, true);
        }

        if (status.isTaken()) {
            try {
                // Desconto de estoque do módulo de medicamentos (integrante 1).
                medication = medicationService.confirmDose(medicationId);
            } catch (RuntimeException exception) {
                repository.restore(medicationId, scheduledAt, previous);
                throw exception;
            }
        }
        return new DoseRecordResult(currentEntry(medicationId, scheduledAt), medication, false);
    }

    public List<MedicationHistory> findByPeriod(String period, String reference) {
        Range range = range(period, reference);
        return repository.findByScheduledBetween(format(range.start()), format(range.endExclusive()));
    }

    public HistoryStats stats(String period, String reference) {
        Range range = range(period, reference);
        List<MedicationHistory> entries =
                repository.findByScheduledBetween(format(range.start()), format(range.endExclusive()));

        // Doses previstas = horários da grade no período (até agora) ∪ doses registradas fora da grade.
        Set<String> expected = new HashSet<>();
        // Só contam os horários que já chegaram: até "agora" ou até o fim do período, o que vier antes.
        LocalDateTime lastMinute = range.endExclusive().minusMinutes(1);
        LocalDateTime limit = lastMinute.isBefore(range.now()) ? lastMinute : range.now();
        for (Medication medication : medicationService.findAll()) {
            addScheduledDoses(expected, medication, range.start(), limit);
        }
        int tomado = 0, atrasado = 0, adiado = 0, naoTomado = 0;
        for (MedicationHistory entry : entries) {
            expected.add(key(entry.getMedicationId(), entry.getScheduledAt()));
            switch (entry.getStatus()) {
                case TOMADO -> tomado++;
                case ATRASADO -> atrasado++;
                case ADIADO -> adiado++;
                case NAO_TOMADO -> naoTomado++;
            }
        }
        int taken = tomado + atrasado;
        int expectedCount = expected.size();
        int adherence = expectedCount == 0 ? 0
                : (int) Math.min(100, Math.round(taken * 100.0 / expectedCount));
        return new HistoryStats(range.period(), range.start().toLocalDate().toString(),
                range.endExclusive().toLocalDate().minusDays(1).toString(),
                expectedCount, taken, adherence, tomado, atrasado, adiado, naoTomado);
    }

    private void addScheduledDoses(Set<String> expected, Medication medication,
                                   LocalDateTime from, LocalDateTime untilInclusive) {
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(medication.getStartDate());
            end = LocalDate.parse(medication.getEndDate());
        } catch (DateTimeParseException | NullPointerException exception) {
            return;
        }
        if (medication.getDoseTimes() == null) {
            return;
        }
        LocalDate first = from.toLocalDate().isAfter(start) ? from.toLocalDate() : start;
        LocalDate last = untilInclusive.toLocalDate().isBefore(end) ? untilInclusive.toLocalDate() : end;
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            for (String time : medication.getDoseTimes()) {
                LocalTime parsedTime;
                try {
                    parsedTime = LocalTime.parse(time.trim());
                } catch (DateTimeParseException exception) {
                    continue;
                }
                LocalDateTime dose = day.atTime(parsedTime);
                if (!dose.isBefore(from) && !dose.isAfter(untilInclusive)) {
                    expected.add(key(medication.getId(), format(dose)));
                }
            }
        }
    }

    private Range range(String period, String reference) {
        LocalDateTime now = isBlank(reference) ? LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                : parseDateTime(reference, "A data de referência");
        LocalDate today = now.toLocalDate();
        String normalized = isBlank(period) ? "day" : period.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "day" -> new Range(normalized, today.atStartOfDay(), today.plusDays(1).atStartOfDay(), now);
            case "week" -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new Range(normalized, monday.atStartOfDay(), monday.plusWeeks(1).atStartOfDay(), now);
            }
            case "month" -> {
                LocalDate first = today.withDayOfMonth(1);
                yield new Range(normalized, first.atStartOfDay(), first.plusMonths(1).atStartOfDay(), now);
            }
            default -> {
                badRequest("Período inválido. Use day, week ou month.");
                yield null;
            }
        };
    }

    private MedicationHistory currentEntry(String medicationId, String scheduledAt) {
        return repository.findByDose(medicationId, scheduledAt)
                .orElseThrow(() -> new IllegalStateException("Registro de histórico não encontrado."));
    }

    private LocalDateTime parseDateTime(String value, String label) {
        if (isBlank(value)) {
            badRequest(label + " é obrigatório.");
        }
        try {
            // Data/hora local enviada pelo app; não há conversão de fuso. Segundos são descartados.
            return LocalDateTime.parse(value.trim()).truncatedTo(ChronoUnit.MINUTES);
        } catch (DateTimeParseException exception) {
            badRequest(label + " deve usar o formato AAAA-MM-DDTHH:mm.");
            return null;
        }
    }

    private DoseStatus parseStatus(String value) {
        if (isBlank(value)) {
            badRequest("Informe o status da dose.");
        }
        try {
            return DoseStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            badRequest("Status inválido. Use TOMADO, ATRASADO, ADIADO ou NAO_TOMADO.");
            return null;
        }
    }

    private static String format(LocalDateTime value) {
        return value.format(DATE_TIME_FORMAT);
    }

    private static String key(String medicationId, String scheduledAt) {
        return medicationId + "|" + scheduledAt;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record Range(String period, LocalDateTime start, LocalDateTime endExclusive, LocalDateTime now) {
    }
}
