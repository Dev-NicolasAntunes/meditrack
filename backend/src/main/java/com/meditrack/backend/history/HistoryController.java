// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DoseRecordResult> record(@RequestBody HistoryRequest request) {
        DoseRecordResult result = service.record(request);
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }

    /**
     * reference é a data/hora local do aparelho (AAAA-MM-DDTHH:mm); sem ela, usa o relógio do servidor.
     */
    @GetMapping
    public List<MedicationHistory> findByPeriod(@RequestParam(defaultValue = "day") String period,
                                                @RequestParam(required = false) String reference) {
        return service.findByPeriod(period, reference);
    }

    @GetMapping("/stats")
    public HistoryStats stats(@RequestParam(defaultValue = "day") String period,
                              @RequestParam(required = false) String reference) {
        return service.stats(period, reference);
    }
}
