package com.nguyenxuanviet.backend.repository;

import com.nguyenxuanviet.backend.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    
    @Query("SELECT t FROM Telemetry t WHERE t.device.id = :deviceId ORDER BY t.timestamp DESC")
    List<Telemetry> findByDeviceIdOrderByTimestampDesc(@Param("deviceId") Long deviceId);
    
    @Query("SELECT t FROM Telemetry t WHERE t.device.id = :deviceId ORDER BY t.timestamp DESC LIMIT 20")
    List<Telemetry> findTop20ByDeviceIdOrderByTimestampDesc(@Param("deviceId") Long deviceId);
}
