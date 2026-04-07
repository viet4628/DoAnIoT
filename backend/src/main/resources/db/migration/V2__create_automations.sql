-- Create automations table
CREATE TABLE IF NOT EXISTS automations (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    task_count INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    icon_config TEXT,
    card_color VARCHAR(20),
    card_icon VARCHAR(100),
    trigger_config TEXT,
    action_config TEXT,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample automation data
INSERT INTO automations (title, type, task_count, is_active, icon_config, display_order) VALUES
('Turn on the AC', 'automation', 1, true, '[{"icon":"device_thermostat","color":"#FF3B30"},{"icon":"water_drop_rounded","color":"#32ADE6"},{"isArrow":true},{"icon":"ac_unit_rounded","color":"#32ADE6"}]', 1),
('Welcome Home Automation', 'automation', 1, true, '[{"icon":"login_rounded","color":"#34C759"},{"isArrow":true},{"icon":"wb_sunny_rounded","color":"#FF9500"}]', 2),
('Bedtime Bliss Automation', 'automation', 2, true, '[{"icon":"access_time_filled_rounded","color":"#34C759"},{"isArrow":true},{"icon":"access_time_filled_rounded","color":"#007AFF"},{"icon":"wb_sunny_rounded","color":"#FF9500"}]', 3),
('Turn ON All the Lights', 'automation', 1, true, '[{"icon":"access_time_filled_rounded","color":"#34C759"},{"isArrow":true},{"icon":"wb_sunny_rounded","color":"#FF9500"}]', 4),
('Go to Office', 'automation', 1, false, '[{"icon":"location_on","color":"#FF3B30"},{"isArrow":true},{"icon":"business_center","color":"#5AC8FA"}]', 5);

INSERT INTO automations (title, type, task_count, card_color, card_icon, display_order) VALUES
('Bedtime Prep', 'tap-to-run', 2, '#2D9AE7', 'nightlight_round', 6),
('Evening Chill', 'tap-to-run', 4, '#8CC255', 'wb_sunny_outlined', 7),
('Boost Productivity', 'tap-to-run', 1, '#9F2DB5', 'query_stats', 8),
('Get Energized', 'tap-to-run', 3, '#FF453A', 'local_fire_department', 9),
('Home Office', 'tap-to-run', 2, '#1DB8CC', 'home', 10),
('Reading Corner', 'tap-to-run', 4, '#7F5D4F', 'menu_book', 11),
('Outdoor Party', 'tap-to-run', 3, '#6E8796', 'celebration', 12);
