package com.nguyenxuanviet.backend.service;

import com.nguyenxuanviet.backend.config.DataInitializer;
import com.nguyenxuanviet.backend.model.Device;
import com.nguyenxuanviet.backend.repository.DeviceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final MqttPublisherService mqttPublisher;
    private final MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter;

    public DeviceService(DeviceRepository deviceRepository,
                         MqttPublisherService mqttPublisher,
                         MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter) {
        this.deviceRepository = deviceRepository;
        this.mqttPublisher = mqttPublisher;
        this.mqttInboundAdapter = mqttInboundAdapter;
    }

    // Removed @Cacheable for real-time IoT updates via MQTT
    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    // Removed @Cacheable for real-time IoT updates via MQTT
    public Optional<Device> findById(Long id) {
        return deviceRepository.findById(id);
    }

    @CacheEvict(value = "devices", allEntries = true)
    public Device create(Device device) {
        Device saved = deviceRepository.save(device);

        if (saved.getStatusTopic() != null && !saved.getStatusTopic().isBlank()) {
            try {
                mqttInboundAdapter.addTopic(saved.getStatusTopic(), 1);
                System.out.println("[DEVICE] Subscribed to status topic: " + saved.getStatusTopic());
            } catch (Exception e) {
                System.err.println("[DEVICE] Could not subscribe to topic: " + e.getMessage());
            }
        }
        return saved;
    }

    @CacheEvict(value = "devices", allEntries = true)
    public Device update(Long id, Device updated) {
        return deviceRepository.findById(id).map(device -> {
            device.setName(updated.getName());
            if (updated.getCategory() != null) device.setCategory(updated.getCategory());
            if (updated.getImagePath() != null) device.setImagePath(updated.getImagePath());
            if (updated.getControlTopic() != null) device.setControlTopic(updated.getControlTopic());
            if (updated.getStatusTopic() != null) device.setStatusTopic(updated.getStatusTopic());
            return deviceRepository.save(device);
        }).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy device id: " + id));
    }


    public void control(Long id, String command) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy device id: " + id));
        if (device.getControlTopic() == null || device.getControlTopic().isBlank()) {
            throw new IllegalStateException("Device không có controlTopic");
        }
        mqttPublisher.publish(device.getControlTopic(), command);
        System.out.println("[DEVICE] Published '" + command + "' to " + device.getControlTopic());
    }

    @CacheEvict(value = "devices", allEntries = true)
    public void delete(Long id) {

        deviceRepository.deleteById(id);
    }
}

