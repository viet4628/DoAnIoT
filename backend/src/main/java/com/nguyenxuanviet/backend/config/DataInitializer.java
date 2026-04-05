package com.nguyenxuanviet.backend.config;

import com.nguyenxuanviet.backend.model.Device;
import com.nguyenxuanviet.backend.model.Room;
import com.nguyenxuanviet.backend.repository.DeviceRepository;
import com.nguyenxuanviet.backend.repository.RoomRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    public static final String TEST_LAMP_CONTROL_TOPIC = "/home/relay/1/control";
    public static final String TEST_LAMP_STATUS_TOPIC = "/home/relay/1/status";

    @Bean
    public ApplicationRunner seedTestLamp(DeviceRepository deviceRepository,
                                          RoomRepository roomRepository) {
        return args -> {
            if (deviceRepository.findByStatusTopic(TEST_LAMP_STATUS_TOPIC).isPresent()) {
                return;
            }

            Room savedRoom = roomRepository.findByName("Living Room")
                    .orElseGet(() -> {
                        Room room = new Room();
                        room.setName("Living Room");
                        room.setIcon("lamp");
                        return roomRepository.save(room);
                    });

            Device lamp = new Device();
            lamp.setName("Smart Lamp");
            lamp.setCategory("Lightning");
            lamp.setImagePath("lib/shared/image/den.png");
            lamp.setConnectionType("Wi-Fi");
            lamp.setControlTopic(TEST_LAMP_CONTROL_TOPIC);
            lamp.setStatusTopic(TEST_LAMP_STATUS_TOPIC);
            lamp.setOn(false);
            lamp.setOnline(false);
            lamp.setRoom(savedRoom);
            deviceRepository.save(lamp);
        };
    }
}