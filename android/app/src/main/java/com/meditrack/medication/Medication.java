package com.meditrack.medication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Medication {
    private final String id;
    private final String name;
    private final String dosage;
    private final int quantity;
    private final String frequency;
    private final List<String> doseTimes;
    private final String startDate;
    private final String endDate;
    private final int lowStockThreshold;
    private final boolean lowStock;
    private final int estimatedDaysRemaining;

    public Medication(String id, String name, String dosage, int quantity, String frequency,
                      List<String> doseTimes, String startDate, String endDate, int lowStockThreshold) {
        this(id, name, dosage, quantity, frequency, doseTimes, startDate, endDate,
                lowStockThreshold, quantity <= lowStockThreshold,
                doseTimes == null || doseTimes.isEmpty() ? 0
                        : (quantity + doseTimes.size() - 1) / doseTimes.size());
    }

    private Medication(String id, String name, String dosage, int quantity, String frequency,
                       List<String> doseTimes, String startDate, String endDate, int lowStockThreshold,
                       boolean lowStock, int estimatedDaysRemaining) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.quantity = quantity;
        this.frequency = frequency;
        this.doseTimes = doseTimes == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(doseTimes));
        this.startDate = startDate;
        this.endDate = endDate;
        this.lowStockThreshold = lowStockThreshold;
        this.lowStock = lowStock;
        this.estimatedDaysRemaining = estimatedDaysRemaining;
    }

    public static Medication fromJson(JSONObject json) throws JSONException {
        JSONArray jsonTimes = json.optJSONArray("doseTimes");
        List<String> times = new ArrayList<>();
        if (jsonTimes != null) {
            for (int i = 0; i < jsonTimes.length(); i++) {
                times.add(jsonTimes.getString(i));
            }
        }
        int quantity = json.getInt("quantity");
        int threshold = json.getInt("lowStockThreshold");
        return new Medication(
                json.getString("id"),
                json.getString("name"),
                json.getString("dosage"),
                quantity,
                json.getString("frequency"),
                times,
                json.getString("startDate"),
                json.getString("endDate"),
                threshold,
                json.optBoolean("lowStock", quantity <= threshold),
                json.optInt("estimatedDaysRemaining",
                        times.isEmpty() ? 0 : (quantity + times.size() - 1) / times.size())
        );
    }

    public JSONObject toRequestJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("dosage", dosage);
            json.put("quantity", quantity);
            json.put("frequency", frequency);
            json.put("doseTimes", new JSONArray(doseTimes));
            json.put("startDate", startDate);
            json.put("endDate", endDate);
            json.put("lowStockThreshold", lowStockThreshold);
        } catch (JSONException exception) {
            throw new IllegalStateException("Não foi possível preparar os dados do medicamento.", exception);
        }
        return json;
    }

    public JSONObject toJson() {
        JSONObject json = toRequestJson();
        try {
            json.put("id", id);
            json.put("lowStock", lowStock);
            json.put("estimatedDaysRemaining", estimatedDaysRemaining);
        } catch (JSONException exception) {
            throw new IllegalStateException("Não foi possível preparar os dados do medicamento.", exception);
        }
        return json;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDosage() { return dosage; }
    public int getQuantity() { return quantity; }
    public String getFrequency() { return frequency; }
    public List<String> getDoseTimes() { return doseTimes; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public boolean isLowStock() { return lowStock; }
    public int getEstimatedDaysRemaining() { return estimatedDaysRemaining; }
}
