package com.nguyenxuanviet.backend.service;

import com.nguyenxuanviet.backend.config.DataInitializer;
import com.nguyenxuanviet.backend.model.Device;
import com.nguyenxuanviet.backend.model.Room;
import com.nguyenxuanviet.backend.repository.DeviceRepository;
import com.nguyenxuanviet.backend.repository.RoomRepository;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final MqttPublisherService mqttPublisher;
    private final MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter;

    public DeviceService(DeviceRepository deviceRepository,
                         RoomRepository roomRepository,
                         MqttPublisherService mqttPublisher,
                         MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter) {
        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
        this.mqttPublisher = mqttPublisher;
        this.mqttInboundAdapter = mqttInboundAdapter;
    }

    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public List<Device> findByRoomId(Long roomId) {
        return deviceRepository.findByRoomId(roomId);
    }

    public Optional<Device> findById(Long id) {
        return deviceRepository.findById(id);
    }

    public Optional<Device> findTestLamp() {
        return deviceRepository.findByStatusTopic(DataInitializer.TEST_LAMP_STATUS_TOPIC);
    }

    public Device create(Device device, Long roomId) {
        if (roomId != null) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy room id: " + roomId));
            device.setRoom(room);
        }
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

    public Device update(Long id, Device updated, Long roomId) {
        return deviceRepository.findById(id).map(device -> {
            device.setName(updated.getName());
            if (updated.getCategory() != null) device.setCategory(updated.getCategory());
            if (updated.getImagePath() != null) device.setImagePath(updated.getImagePath());
            if (updated.getConnectionType() != null) device.setConnectionType(updated.getConnectionType());
            if (updated.getControlTopic() != null) device.setControlTopic(updated.getControlTopic());
            if (updated.getStatusTopic() != null) device.setStatusTopic(updated.getStatusTopic());
            if (roomId != null) {
                roomRepository.findById(roomId).ifPresent(device::setRoom);
            }
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

    public Device controlTestLamp(boolean isOn) {
        Device device = findTestLamp()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đèn test"));
        String command = isOn ? "0" : "1";
        control(device.getId(), command);
        device.setOn(isOn);
        device.setOnline(true);
        return deviceRepository.save(device);
    }

    public void delete(Long id) {
        deviceRepository.deleteById(id);
    }
}
