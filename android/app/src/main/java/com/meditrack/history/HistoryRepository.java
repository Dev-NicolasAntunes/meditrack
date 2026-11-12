// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import android.os.Handler;
import android.os.Looper;

import com.meditrack.medication.MedicationRepository;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Acesso ao histórico (tabela medication_history no SQLite do backend).
 * Usa o mesmo Callback do MedicationRepository para manter o padrão das telas.
 */
public class HistoryRepository {
    public static final String PERIOD_DAY = "day";
    public static final String PERIOD_WEEK = "week";
    public static final String PERIOD_MONTH = "month";

    private interface Request<T> {
        T run() throws IOException, JSONException;
    }

    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final HistoryApiClient apiClient = new HistoryApiClient();

    public void record(String medicationId, String scheduledAt, String actionAt, DoseStatus status,
                       MedicationRepository.Callback<DoseRecordResult> callback) {
        execute(() -> apiClient.record(medicationId, scheduledAt, actionAt, status), callback);
    }

    public void findByPeriod(String period, MedicationRepository.Callback<List<MedicationHistory>> callback) {
        String reference = DoseTimes.format(DoseTimes.now());
        execute(() -> apiClient.findByPeriod(period, reference), callback);
    }

    public void stats(String period, MedicationRepository.Callback<HistoryStats> callback) {
        String reference = DoseTimes.format(DoseTimes.now());
        execute(() -> apiClient.stats(period, reference), callback);
    }

    /** Histórico de um período qualquer (atual ou passado). */
    public void findByPeriod(HistoryPeriod period, MedicationRepository.Callback<List<MedicationHistory>> callback) {
        String type = period.getType();
        String reference = period.reference();
        execute(() -> apiClient.findByPeriod(type, reference), callback);
    }

    /** Estatísticas de um período qualquer (atual ou passado). */
    public void stats(HistoryPeriod period, MedicationRepository.Callback<HistoryStats> callback) {
        String type = period.getType();
        String reference = period.reference();
        execute(() -> apiClient.stats(type, reference), callback);
    }

    private <T> void execute(Request<T> request, MedicationRepository.Callback<T> callback) {
        IO.execute(() -> {
            try {
                T result = request.run();
                MAIN.post(() -> callback.onSuccess(result));
            } catch (Exception exception) {
                String message = exception.getMessage();
                MAIN.post(() -> callback.onError(
                        message == null || message.trim().isEmpty()
                                ? "Não foi possível se comunicar com o servidor."
                                : message
                ));
            }
        });
    }
}
