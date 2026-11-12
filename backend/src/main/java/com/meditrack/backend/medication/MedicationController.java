// AI assistance: ChatGPT (OpenAI) assisted with implementation; reviewed and adapted by Nicolas.
package com.meditrack.backend.medication;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {
    private final MedicationService service;

    public MedicationController(MedicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Medication> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Medication findById(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Medication> create(@RequestBody MedicationRequest request) {
        Medication medication = service.create(request);
        return ResponseEntity.created(URI.create("/api/medications/" + medication.getId())).body(medication);
    }

    @PutMapping("/{id}")
    public Medication update(@PathVariable String id, @RequestBody MedicationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stock-adjustments")
    public Medication adjustStock(@PathVariable String id, @RequestBody StockAdjustmentRequest request) {
        return service.adjustStock(id, request.quantityChange());
    }

    @PostMapping("/{id}/doses/confirm")
    public Medication confirmDose(@PathVariable String id) {
        return service.confirmDose(id);
    }
}
