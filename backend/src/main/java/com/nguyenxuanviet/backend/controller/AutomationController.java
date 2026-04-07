package com.nguyenxuanviet.backend.controller;

import com.nguyenxuanviet.backend.model.Automation;
import com.nguyenxuanviet.backend.service.AutomationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/automations")
public class AutomationController {

    private final AutomationService automationService;

    public AutomationController(AutomationService automationService) {
        this.automationService = automationService;
    }

    @GetMapping
    public List<Automation> getAll() {
        return automationService.findAll();
    }

    @GetMapping("/type/{type}")
    public List<Automation> getByType(@PathVariable String type) {
        return automationService.findByType(type);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Automation> getById(@PathVariable Long id) {
        return automationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Automation automation) {
        try {
            if (automation.getTitle() == null || automation.getTitle().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "title là bắt buộc"));
            }
            if (automation.getType() == null || automation.getType().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "type là bắt buộc"));
            }
            
            Automation saved = automationService.create(automation);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Automation automation) {
        try {
            return ResponseEntity.ok(automationService.update(id, automation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        automationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
