import React, { useState, useEffect } from 'react';
import { deviceApi } from '../services/api';
import '../styles/DeviceCard.css';

const DeviceCard = ({ device, onUpdate }) => {
  const [loading, setLoading] = useState(false);
  const [localState, setLocalState] = useState(device.isOn);

  // Sync local state when device prop changes
  useEffect(() => {
    setLocalState(device.isOn);
  }, [device.isOn]);

  const handleToggle = async () => {
    setLoading(true);
    
    // Save old state before toggling
    const oldState = localState;
    
    // Calculate command FIRST before updating state
    // Send "0" for ON (when currently OFF), "1" for OFF (when currently ON)
    const command = oldState ? "1" : "0";
    
    // Optimistic UI update
    setLocalState(!oldState);
    
    try {
      await deviceApi.controlDevice(device.id, command);
      
      // Refresh from backend after 800ms to confirm state
      setTimeout(onUpdate, 800);
    } catch (error) {
      console.error('Failed to control device:', error);
      alert('Failed to control device: ' + (error.response?.data?.error || error.message));
      
      // Revert to old state on error
      setLocalState(oldState);
    } finally {
      setLoading(false);
    }
  };

  const isOn = localState; // Use local optimistic state

  return (
    <div className={`device-card ${isOn ? 'device-on' : 'device-off'}`}>
      <div className="device-header">
        <h3>{device.name}</h3>
        <span className={`status-badge ${isOn ? 'on' : 'off'}`}>
          {isOn ? 'ON' : 'OFF'}
        </span>
      </div>
      
      <div className="device-info">
        <p><strong>MAC:</strong> {device.macAddress}</p>
        <p><strong>Location:</strong> {device.location || 'N/A'}</p>
        <p><strong>Type:</strong> {device.type || 'Relay'}</p>
      </div>

      <button
        className={`control-btn ${isOn ? 'btn-off' : 'btn-on'}`}
        onClick={handleToggle}
        disabled={loading}
      >
        {loading ? 'Processing...' : isOn ? 'Turn OFF' : 'Turn ON'}
      </button>
    </div>
  );
};

export default DeviceCard;
