import React, { useState, useEffect } from 'react';
import { deviceApi } from '../services/api';
import DeviceCard from '../components/DeviceCard';
import TelemetryHistory from '../components/TelemetryHistory';
import '../styles/Dashboard.css';

const Dashboard = () => {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);

  const fetchDevices = async () => {
    try {
      const response = await deviceApi.getDevices();
      setDevices(response.data);
      setError(null);
      
      // Auto-select first device if none selected
      if (!selectedDevice && response.data.length > 0) {
        setSelectedDevice(response.data[0]);
      }
    } catch (err) {
      console.error('Failed to fetch devices:', err);
      setError('Failed to load devices');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDevices();
    
    // Auto-refresh every 2 seconds (match Flutter app)
    const interval = setInterval(fetchDevices, 2000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) {
    return (
      <div className="dashboard">
        <div className="loading">Loading devices...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard">
        <div className="error">{error}</div>
        <button onClick={fetchDevices}>Retry</button>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Smart Home Dashboard</h1>
        <button className="refresh-btn" onClick={fetchDevices}>
          Refresh
        </button>
      </div>

      <div className="stats">
        <div className="stat-card">
          <h3>Total Devices</h3>
          <p className="stat-value">{devices.length}</p>
        </div>
        <div className="stat-card">
          <h3>Active</h3>
          <p className="stat-value">{devices.filter(d => d.isOn).length}</p>
        </div>
        <div className="stat-card">
          <h3>Offline</h3>
          <p className="stat-value">{devices.filter(d => !d.isOnline).length}</p>
        </div>
      </div>

      <div className="dashboard-content">
        <div className="devices-section">
          <h2>Devices</h2>
          <div className="devices-grid">
            {devices.length === 0 ? (
              <p className="no-devices">No devices found. Register a device using the mobile app.</p>
            ) : (
              devices.map((device) => (
                <div 
                  key={device.id}
                  onClick={() => setSelectedDevice(device)}
                  className={selectedDevice?.id === device.id ? 'device-selected' : ''}
                >
                  <DeviceCard
                    device={device}
                    onUpdate={fetchDevices}
                  />
                </div>
              ))
            )}
          </div>
        </div>

        <div className="telemetry-section">
          <TelemetryHistory 
            deviceId={selectedDevice?.id}
            deviceName={selectedDevice?.name}
          />
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
