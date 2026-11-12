// AI assistance: ChatGPT (OpenAI) assisted with implementation; reviewed and adapted by Nicolas.
package com.meditrack.medication;

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

public class MedicationApiClient {
    public List<Medication> getMedications() throws IOException, JSONException {
        JSONArray json = new JSONArray(request("GET", "medications", null));
        List<Medication> medications = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            medications.add(Medication.fromJson(json.getJSONObject(i)));
        }
        return medications;
    }

    public Medication getMedication(String id) throws IOException, JSONException {
        return Medication.fromJson(new JSONObject(request("GET", path(id), null)));
    }

    public Medication saveMedication(String id, JSONObject body) throws IOException, JSONException {
        String method = id == null ? "POST" : "PUT";
        String endpoint = id == null ? "medications" : path(id);
        return Medication.fromJson(new JSONObject(request(method, endpoint, body)));
    }

    public void deleteMedication(String id) throws IOException, JSONException {
        request("DELETE", path(id), null);
    }

    public Medication adjustStock(String id, int quantityChange) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("quantityChange", quantityChange);
        return Medication.fromJson(new JSONObject(request("POST", path(id) + "/stock-adjustments", body)));
    }

    public Medication confirmDose(String id) throws IOException, JSONException {
        return Medication.fromJson(new JSONObject(request("POST", path(id) + "/doses/confirm", new JSONObject())));
    }

    private String path(String id) {
        return "medications/" + Uri.encode(id);
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
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private String errorMessage(String body, int status) {
        try {
            JSONObject json = new JSONObject(body);
            String message = json.optString("detail");
            if (message.isEmpty()) {
                message = json.optString("message");
            }
            return message.isEmpty() ? "Erro HTTP " + status : message;
        } catch (JSONException exception) {
            return "Erro HTTP " + status;
        }
    }
}
