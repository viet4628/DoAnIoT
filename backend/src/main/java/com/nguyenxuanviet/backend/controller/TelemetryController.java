package com.nguyenxuanviet.backend.controller;

import com.nguyenxuanviet.backend.model.Telemetry;
import com.nguyenxuanviet.backend.repository.TelemetryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryRepository telemetryRepository;

    public TelemetryController(TelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<List<Telemetry>> getByDevice(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "20") int limit) {
        limit = Math.min(limit, 200); // Cap at 200 to prevent abuse
        return ResponseEntity.ok(
                telemetryRepository.findTop20ByDeviceIdOrderByTimestampDesc(deviceId)
                        .stream()
                        .limit(limit)
                        .toList()
        );
    }

    @GetMapping("/{deviceId}/all")
    public List<Telemetry> getAllByDevice(@PathVariable Long deviceId) {
        return telemetryRepository.findByDeviceIdOrderByTimestampDesc(deviceId);
    }
}
