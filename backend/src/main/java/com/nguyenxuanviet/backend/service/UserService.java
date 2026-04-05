package com.nguyenxuanviet.backend.service;

import com.nguyenxuanviet.backend.model.User;
import com.nguyenxuanviet.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' đã tồn tại");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email '" + user.getEmail() + "' đã tồn tại");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()));
    }

    public User update(Long id, User updated) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updated.getUsername());
            user.setEmail(updated.getEmail());
            if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(updated.getPassword()));
            }
            return userRepository.save(user);
        }).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id: " + id));
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
