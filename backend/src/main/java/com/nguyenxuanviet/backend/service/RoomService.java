package com.nguyenxuanviet.backend.service;

import com.nguyenxuanviet.backend.model.Room;
import com.nguyenxuanviet.backend.model.User;
import com.nguyenxuanviet.backend.repository.RoomRepository;
import com.nguyenxuanviet.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public RoomService(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    public List<Room> findByUserId(Long userId) {
        return roomRepository.findByUserId(userId);
    }

    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    public Room create(Room room, Long userId) {
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id: " + userId));
            room.setUser(user);
        }
        return roomRepository.save(room);
    }

    public Room update(Long id, Room updated) {
        return roomRepository.findById(id).map(room -> {
            room.setName(updated.getName());
            if (updated.getIcon() != null) room.setIcon(updated.getIcon());
            return roomRepository.save(room);
        }).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy room id: " + id));
    }

    public void delete(Long id) {
        roomRepository.deleteById(id);
    }
}
