import React, { useState, useEffect, useCallback } from 'react';
import { deviceApi } from '../services/api';
import { format } from 'date-fns';
import '../styles/TelemetryHistory.css';

const TelemetryHistory = ({ deviceId, deviceName }) => {
  const [telemetry, setTelemetry] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchTelemetry = useCallback(async () => {
    if (!deviceId) return;
    
    try {
      const response = await deviceApi.getTelemetry(deviceId, 50);
      setTelemetry(response.data);
    } catch (error) {
      console.error('Failed to fetch telemetry:', error);
    } finally {
      setLoading(false);
    }
  }, [deviceId]);

  useEffect(() => {
    fetchTelemetry();
    
    // Refresh every 3 seconds
    const interval = setInterval(fetchTelemetry, 3000);
    return () => clearInterval(interval);
  }, [fetchTelemetry]);

  if (!deviceId) {
    return (
      <div className="telemetry-history">
        <div className="telemetry-header">
          <h2>Device History</h2>
        </div>
        <div className="telemetry-empty">
          Select a device to view history
        </div>
      </div>
    );
  }

  return (
    <div className="telemetry-history">
      <div className="telemetry-header">
        <h2>{deviceName || 'Device'} History</h2>
        <button className="refresh-btn-small" onClick={fetchTelemetry}>
          ↻
        </button>
      </div>

      {loading ? (
        <div className="telemetry-loading">Loading history...</div>
      ) : telemetry.length === 0 ? (
        <div className="telemetry-empty">No history available</div>
      ) : (
        <div className="telemetry-list">
          {telemetry.map((item, index) => {
            const isOn = item.value === "0";
            const timestamp = new Date(item.timestamp);
            
            return (
              <div key={index} className="telemetry-item">
                <div className={`status-indicator ${isOn ? 'on' : 'off'}`}>
                  {isOn ? '●' : '○'}
                </div>
                <div className="telemetry-content">
                  <div className="telemetry-state">
                    {isOn ? 'Turned ON' : 'Turned OFF'}
                  </div>
                  <div className="telemetry-time">
                    {format(timestamp, 'HH:mm:ss - dd/MM/yyyy')}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default TelemetryHistory;
