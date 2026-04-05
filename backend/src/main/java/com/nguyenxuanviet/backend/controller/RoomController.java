package com.nguyenxuanviet.backend.controller;

import com.nguyenxuanviet.backend.model.Room;
import com.nguyenxuanviet.backend.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<Room> getAll(@RequestParam(required = false) Long userId) {
        if (userId != null) return roomService.findByUserId(userId);
        return roomService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getById(@PathVariable Long id) {
        return roomService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Room room,
                                    @RequestParam(required = false) Long userId) {
        try {
            if (room.getName() == null || room.getName().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "name là bắt buộc"));
            Room saved = roomService.create(room, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Room room) {
        try {
            return ResponseEntity.ok(roomService.update(id, room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
