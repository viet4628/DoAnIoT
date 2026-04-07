package com.nguyenxuanviet.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "automations")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Automation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type; // "automation" hoặc "tap-to-run"

    @Column(name = "task_count")
    private int taskCount = 1;

    @Column(name = "is_active")
    @JsonProperty("isActive")
    private boolean isActive = true;

    @Column(name = "icon_config", columnDefinition = "TEXT")
    private String iconConfig; // JSON string chứa icons

    @Column(name = "card_color")
    private String cardColor; // Hex color cho tap-to-run cards

    @Column(name = "card_icon")
    private String cardIcon; // Icon name cho tap-to-run cards

    @Column(name = "trigger_config", columnDefinition = "TEXT")
    private String triggerConfig; // JSON string chứa trigger conditions

    @Column(name = "action_config", columnDefinition = "TEXT")
    private String actionConfig; // JSON string chứa actions

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
