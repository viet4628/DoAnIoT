package com.nguyenxuanviet.backend.repository;

import com.nguyenxuanviet.backend.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    List<Telemetry> findByDeviceIdOrderByTimestampDesc(Long deviceId);
    List<Telemetry> findTop20ByDeviceIdOrderByTimestampDesc(Long deviceId);
}
