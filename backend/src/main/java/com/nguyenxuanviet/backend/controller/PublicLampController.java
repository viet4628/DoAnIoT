package com.nguyenxuanviet.backend.controller;

import com.nguyenxuanviet.backend.model.Device;
import com.nguyenxuanviet.backend.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/lamp")
public class PublicLampController {

    private final DeviceService deviceService;

    public PublicLampController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<?> getLamp() {
        return deviceService.findTestLamp()
                .map(device -> ResponseEntity.ok(toLampResponse(device)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/control")
    public ResponseEntity<?> controlLamp(@RequestBody Map<String, Object> body) {
        Object isOnValue = body.get("isOn");
        if (!(isOnValue instanceof Boolean isOn)) {
            return ResponseEntity.badRequest().body(Map.of("error", "isOn phải là boolean"));
        }

        Device device = deviceService.controlTestLamp(isOn);
        return ResponseEntity.ok(toLampResponse(device));
    }

    private Map<String, Object> toLampResponse(Device device) {
        return Map.of(
                "backendId", device.getId(),
                "name", device.getName(),
                "imagePath", device.getImagePath() == null ? "lib/shared/image/den.png" : device.getImagePath(),
                "category", device.getCategory() == null ? "Lightning" : device.getCategory(),
                "room", device.getRoom() == null ? "Living Room" : device.getRoom().getName(),
                "connectionType", device.getConnectionType() == null ? "Wi-Fi" : device.getConnectionType(),
                "isOn", device.isOn(),
                "isOnline", device.isOnline()
        );
    }
}