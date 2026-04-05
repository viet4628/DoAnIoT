package com.nguyenxuanviet.backend.controller;

import com.nguyenxuanviet.backend.model.Device;
import com.nguyenxuanviet.backend.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<Device> getAll(@RequestParam(required = false) Long roomId) {
        if (roomId != null) return deviceService.findByRoomId(roomId);
        return deviceService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getById(@PathVariable Long id) {
        return deviceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Device device,
                                    @RequestParam(required = false) Long roomId) {
        try {
            if (device.getName() == null || device.getName().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "name là bắt buộc"));
            Device saved = deviceService.create(device, roomId);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Device device,
                                    @RequestParam(required = false) Long roomId) {
        try {
            return ResponseEntity.ok(deviceService.update(id, device, roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/control")
    public ResponseEntity<?> control(@PathVariable Long id, @RequestBody String command) {
        try {
            deviceService.control(id, command.trim());
            return ResponseEntity.ok(Map.of(
                    "message", "Command sent",
                    "deviceId", id,
                    "command", command.trim()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
