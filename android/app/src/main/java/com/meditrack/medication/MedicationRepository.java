// AI assistance: ChatGPT (OpenAI) assisted with implementation; reviewed and adapted by Nicolas.
package com.meditrack.medication;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedicationRepository {
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private interface Request<T> {
        T run() throws IOException, JSONException;
    }

    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final MedicationApiClient apiClient = new MedicationApiClient();

    public void getAll(Callback<List<Medication>> callback) {
        execute(apiClient::getMedications, callback);
    }

    public void getById(String id, Callback<Medication> callback) {
        execute(() -> apiClient.getMedication(id), callback);
    }

    public void save(String id, JSONObject body, Callback<Medication> callback) {
        execute(() -> apiClient.saveMedication(id, body), callback);
    }

    public void delete(String id, Callback<Void> callback) {
        execute(() -> {
            apiClient.deleteMedication(id);
            return null;
        }, callback);
    }

    public void adjustStock(String id, int change, Callback<Medication> callback) {
        execute(() -> apiClient.adjustStock(id, change), callback);
    }

    public void confirmDose(String id, Callback<Medication> callback) {
        execute(() -> apiClient.confirmDose(id), callback);
    }

    private <T> void execute(Request<T> request, Callback<T> callback) {
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
