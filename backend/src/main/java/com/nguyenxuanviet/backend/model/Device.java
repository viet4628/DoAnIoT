package com.nguyenxuanviet.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    private String imagePath;

    @Column(name = "control_topic")
    private String controlTopic;

    @Column(name = "status_topic", unique = true)
    private String statusTopic;

    private boolean isOn = false;
    private boolean isOnline = false;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
