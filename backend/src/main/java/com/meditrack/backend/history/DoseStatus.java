// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

public enum DoseStatus {
    TOMADO,
    ATRASADO,
    ADIADO,
    NAO_TOMADO;

    public boolean isTaken() {
        return this == TOMADO || this == ATRASADO;
    }
}
