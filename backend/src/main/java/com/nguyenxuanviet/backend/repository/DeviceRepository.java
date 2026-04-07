package com.nguyenxuanviet.backend.repository;

import com.nguyenxuanviet.backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByStatusTopic(String statusTopic);
}
