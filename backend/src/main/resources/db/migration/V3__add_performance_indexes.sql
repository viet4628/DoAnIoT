-- Performance optimization: Add indexes for frequently queried columns

-- Index for device status_topic (used in MQTT subscriptions)
CREATE INDEX IF NOT EXISTS idx_devices_status_topic ON devices(status_topic);

-- Index for device control_topic
CREATE INDEX IF NOT EXISTS idx_devices_control_topic ON devices(control_topic);

-- Index for telemetry device_id and timestamp (used for history queries)
CREATE INDEX IF NOT EXISTS idx_telemetry_device_timestamp ON telemetry(device_id, timestamp DESC);

-- Index for automation type (used for filtering automation vs tap-to-run)
CREATE INDEX IF NOT EXISTS idx_automations_type ON automations(type);

-- Index for automation display_order (used for sorting)
CREATE INDEX IF NOT EXISTS idx_automations_display_order ON automations(display_order);

-- Index for automation active status (used for filtering active automations)
CREATE INDEX IF NOT EXISTS idx_automations_is_active ON automations(is_active);
