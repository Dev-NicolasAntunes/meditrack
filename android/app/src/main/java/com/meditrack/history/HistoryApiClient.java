// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import android.net.Uri;

import com.meditrack.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Cliente HTTP do histórico; segue o mesmo padrão do MedicationApiClient. Métodos bloqueantes. */
public class HistoryApiClient {
    public DoseRecordResult record(String medicationId, String scheduledAt, String actionAt, DoseStatus status)
            throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("medicationId", medicationId);
        body.put("scheduledAt", scheduledAt);
        body.put("actionAt", actionAt);
        body.put("status", status.name());
        return DoseRecordResult.fromJson(new JSONObject(request("POST", "history", body)));
    }

    public List<MedicationHistory> findByPeriod(String period, String reference) throws IOException, JSONException {
        JSONArray json = new JSONArray(request("GET", "history" + query(period, reference), null));
        List<MedicationHistory> entries = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            entries.add(MedicationHistory.fromJson(json.getJSONObject(i)));
        }
        return entries;
    }

    public HistoryStats stats(String period, String reference) throws IOException, JSONException {
        return HistoryStats.fromJson(new JSONObject(request("GET", "history/stats" + query(period, reference), null)));
    }

    private String query(String period, String reference) {
        return "?period=" + Uri.encode(period) + "&reference=" + Uri.encode(reference);
    }

    private String request(String method, String path, JSONObject body) throws IOException {
        String baseUrl = BuildConfig.API_BASE_URL;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = stream == null ? "" : readFully(stream);
            if (status < 200 || status >= 300) {
                throw new IOException(errorMessage(response, status));
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private String readFully(InputStream stream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private String errorMessage(String body, int status) {
        String message = "";
        try {
            JSONObject json = new JSONObject(body);
            message = json.optString("detail");
            if (message.isEmpty()) {
                message = json.optString("message");
            }
        } catch (JSONException ignored) {
            // Corpo sem JSON: usa a mensagem padrão abaixo.
        }
        if (!message.isEmpty()) {
            return message;
        }
        if (status == 404) {
            return "Medicamento não encontrado.";
        }
        if (status == 409) {
            return "Não há unidades disponíveis no estoque.";
        }
        return "Erro HTTP " + status;
    }
}
