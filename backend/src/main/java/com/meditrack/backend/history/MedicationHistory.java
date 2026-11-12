// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

public class MedicationHistory {
    private long id;
    private String medicationId;
    private String medicationName;
    private String dosage;
    private String scheduledAt;
    private String actionAt;
    private DoseStatus status;

    public MedicationHistory() {
    }

    public MedicationHistory(long id, String medicationId, String medicationName, String dosage,
                             String scheduledAt, String actionAt, DoseStatus status) {
        this.id = id;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.scheduledAt = scheduledAt;
        this.actionAt = actionAt;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getMedicationId() { return medicationId; }
    public void setMedicationId(String medicationId) { this.medicationId = medicationId; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getActionAt() { return actionAt; }
    public void setActionAt(String actionAt) { this.actionAt = actionAt; }
    public DoseStatus getStatus() { return status; }
    public void setStatus(DoseStatus status) { this.status = status; }
}
