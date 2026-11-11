package com.meditrack.backend.medication;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class MedicationService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final MedicationRepository repository;

    public MedicationService(MedicationRepository repository) {
        this.repository = repository;
    }

    public List<Medication> findAll() {
        return repository.findAll();
    }

    public Medication findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento não encontrado."));
    }

    public Medication create(MedicationRequest request) {
        validate(request);
        Medication medication = fromRequest(UUID.randomUUID().toString(), request);
        repository.insert(medication);
        return medication;
    }

    public Medication update(String id, MedicationRequest request) {
        validate(request);
        findById(id);
        Medication medication = fromRequest(id, request);
        if (!repository.update(medication)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento não encontrado.");
        }
        return medication;
    }

    public void delete(String id) {
        if (!repository.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento não encontrado.");
        }
    }

    public Medication adjustStock(String id, int quantityChange) {
        if (quantityChange == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A alteração de estoque não pode ser zero.");
        }
        // A operação deve permanecer dentro do intervalo aceito pelo campo quantity (int).
        // O cálculo em long evita overflow antes de chegar à atualização atômica do repositório.
        Medication current = findById(id);
        long newQuantity = (long) current.getQuantity() + quantityChange;
        if (newQuantity < 0 || newQuantity > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O estoque não pode ficar negativo nem ultrapassar o limite permitido."
            );
        }
        if (!repository.adjustStock(id, quantityChange)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O estoque não pode ficar negativo nem ultrapassar o limite permitido."
            );
        }
        return findById(id);
    }

    public Medication confirmDose(String id) {
        if (!repository.decreaseStockForDose(id)) {
            if (repository.findById(id).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento não encontrado.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não há unidades disponíveis no estoque.");
        }
        return findById(id);
    }

    private Medication fromRequest(String id, MedicationRequest request) {
        List<String> normalizedTimes = request.doseTimes().stream()
                .map(String::trim)
                .sorted()
                .toList();
        return new Medication(
                id,
                request.name().trim(),
                request.dosage().trim(),
                request.quantity(),
                request.frequency().trim(),
                normalizedTimes,
                request.startDate().trim(),
                request.endDate().trim(),
                request.lowStockThreshold()
        );
    }

    private void validate(MedicationRequest request) {
        if (request == null) {
            badRequest("Os dados do medicamento são obrigatórios.");
        }
        if (isBlank(request.name()) || isBlank(request.dosage()) || isBlank(request.frequency())) {
            badRequest("Nome, dosagem e frequência são obrigatórios.");
        }
        if (!request.dosage().trim().matches("(?i)(?:[0-9]+(?:[.,][0-9]+)?)\\s*(?:mg|g|mcg|ml|l|gotas|comprimido\\(s\\)|cápsula\\(s\\))")) {
            badRequest("Informe uma dosagem numérica maior que zero e escolha uma unidade válida (mg, g, mcg, mL, L, gotas, comprimidos ou cápsulas).");
        }
        try {
            if (new java.math.BigDecimal(request.dosage().trim().replaceAll("(?i)\\s*(mg|g|mcg|ml|l|gotas|comprimido\\(s\\)|cápsula\\(s\\))$", "").trim().replace(',', '.')).signum() <= 0) {
                badRequest("A dosagem deve ser maior que zero.");
            }
        } catch (NumberFormatException exception) {
            badRequest("A dosagem deve conter um valor numérico válido.");
        }
        if (request.quantity() < 0) {
            badRequest("A quantidade não pode ser negativa.");
        }
        if (request.lowStockThreshold() < 0) {
            badRequest("O limite de estoque baixo não pode ser negativo.");
        }

        List<String> times = request.doseTimes();
        if (times == null || times.isEmpty()) {
            badRequest("Informe ao menos um horário de utilização.");
        }
        String expectedFrequency = times.size() == 1 ? "1 vez ao dia" : times.size() + " vezes ao dia";
        if (!expectedFrequency.equalsIgnoreCase(request.frequency().trim())) {
            badRequest("A frequência deve corresponder à quantidade de horários selecionados: " + expectedFrequency + ".");
        }
        HashSet<String> uniqueTimes = new HashSet<>();
        for (String time : times) {
            if (isBlank(time)) {
                badRequest("Os horários não podem ficar vazios.");
            }
            try {
                LocalTime parsedTime = LocalTime.parse(time.trim());
                if (parsedTime.getSecond() != 0 || parsedTime.getNano() != 0) {
                    badRequest("Use horários no formato HH:mm.");
                }
            } catch (DateTimeParseException exception) {
                badRequest("Use horários no formato HH:mm.");
            }
            if (!uniqueTimes.add(time.trim())) {
                badRequest("Não repita o mesmo horário.");
            }
        }

        LocalDate start = parseDate(request.startDate(), "A data de início");
        LocalDate end = parseDate(request.endDate(), "A data de término");
        if (end.isBefore(start)) {
            badRequest("A data de término deve ser igual ou posterior à data de início.");
        }
    }

    private LocalDate parseDate(String value, String label) {
        if (isBlank(value)) {
            badRequest(label + " é obrigatória.");
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            badRequest(label + " deve usar o formato AAAA-MM-DD.");
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
