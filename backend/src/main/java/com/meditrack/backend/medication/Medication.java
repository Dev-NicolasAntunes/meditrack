package com.meditrack.backend.medication;

import java.util.List;

public class Medication {
    private String id;
    private String name;
    private String dosage;
    private int quantity;
    private String frequency;
    private List<String> doseTimes;
    private String startDate;
    private String endDate;
    private int lowStockThreshold;

    public Medication() {
    }

    public Medication(String id, String name, String dosage, int quantity, String frequency,
                      List<String> doseTimes, String startDate, String endDate, int lowStockThreshold) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.quantity = quantity;
        this.frequency = frequency;
        this.doseTimes = doseTimes;
        this.startDate = startDate;
        this.endDate = endDate;
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public List<String> getDoseTimes() { return doseTimes; }
    public void setDoseTimes(List<String> doseTimes) { this.doseTimes = doseTimes; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }

    public int getEstimatedDaysRemaining() {
        int dosesPerDay = doseTimes == null ? 0 : doseTimes.size();
        return dosesPerDay == 0 ? 0 : (quantity + dosesPerDay - 1) / dosesPerDay;
    }
}
