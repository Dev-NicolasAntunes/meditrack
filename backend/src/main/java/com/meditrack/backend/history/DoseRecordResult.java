// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

import com.meditrack.backend.medication.Medication;

/**
 * Resultado do registro de uma dose. "duplicate" indica que a dose já estava registrada como
 * tomada e, por isso, o estoque não foi descontado de novo.
 */
public record DoseRecordResult(
        MedicationHistory history,
        Medication medication,
        boolean duplicate
) {
}
