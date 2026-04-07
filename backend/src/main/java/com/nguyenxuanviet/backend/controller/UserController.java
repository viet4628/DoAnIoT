package com.nguyenxuanviet.backend.controller;

import com.nguyenxuanviet.backend.model.User;
import com.nguyenxuanviet.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            if (user.getUsername() == null || user.getUsername().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "username là bắt buộc"));
            if (user.getEmail() == null || user.getEmail().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "email là bắt buộc"));
            if (user.getPassword() == null || user.getPassword().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "password là bắt buộc"));

            User saved = userService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String identifier = body.get("username");
        String password = body.get("password");
        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username/email và password là bắt buộc"));
        }
        return userService.login(identifier, password)

            .map(user -> ResponseEntity.ok((Object) toUserResponse(user)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Sai username hoặc password")));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok((Object) toUserResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updated) {
        try {
            User saved = userService.update(id, updated);
            return ResponseEntity.ok(toUserResponse(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> toUserResponse(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "createdAt", user.getCreatedAt() == null ? "" : user.getCreatedAt().toString()
        );
    }
}
