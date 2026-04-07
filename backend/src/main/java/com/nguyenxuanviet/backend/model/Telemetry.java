package com.nguyenxuanviet.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnore  // Don't serialize the device object, only deviceId
    private Device device;

    private String value;

    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Add deviceId field for JSON serialization
    public Long getDeviceId() {
        return device != null ? device.getId() : null;
    }
}
