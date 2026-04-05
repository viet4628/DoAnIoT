package com.nguyenxuanviet.backend.repository;

import com.nguyenxuanviet.backend.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByUserId(Long userId);
    Optional<Room> findByName(String name);
}
