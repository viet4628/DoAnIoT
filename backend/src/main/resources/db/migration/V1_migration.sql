-- Schema is managed by Hibernate (ddl-auto=update)
-- This file is for reference and manual migrations

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Rooms
CREATE TABLE IF NOT EXISTS rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(100),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);

-- Devices
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    image_path VARCHAR(500),
    connection_type VARCHAR(50),
    control_topic VARCHAR(255),
    status_topic VARCHAR(255) UNIQUE,
    is_on BOOLEAN DEFAULT FALSE,
    is_online BOOLEAN DEFAULT FALSE,
    room_id BIGINT REFERENCES rooms(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Telemetry (MQTT events log)
CREATE TABLE IF NOT EXISTS telemetry (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    value VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for telemetry time-series queries
CREATE INDEX IF NOT EXISTS idx_telemetry_device_time
    ON telemetry (device_id, timestamp DESC);

-- Index for dashboard fast loading
CREATE INDEX IF NOT EXISTS idx_rooms_user ON rooms(user_id);
CREATE INDEX IF NOT EXISTS idx_devices_room ON devices(room_id);