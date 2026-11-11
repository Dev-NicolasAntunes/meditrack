package com.meditrack.backend.medication;

import java.util.List;

public record MedicationRequest(
        String name,
        String dosage,
        int quantity,
        String frequency,
        List<String> doseTimes,
        String startDate,
        String endDate,
        int lowStockThreshold
) {
}
