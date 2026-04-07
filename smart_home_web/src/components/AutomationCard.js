import React, { useState } from 'react';
import { automationApi } from '../services/api';
import { format } from 'date-fns';
import '../styles/AutomationCard.css';

const AutomationCard = ({ automation, onUpdate, onDelete }) => {
  const [loading, setLoading] = useState(false);

  const handleToggle = async () => {
    setLoading(true);
    try {
      await automationApi.toggleAutomation(automation.id, !automation.isActive);
      onUpdate();
    } catch (error) {
      console.error('Failed to toggle automation:', error);
      alert('Failed to toggle automation');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (window.confirm(`Delete automation "${automation.name}"?`)) {
      try {
        await automationApi.deleteAutomation(automation.id);
        onDelete(automation.id);
      } catch (error) {
        console.error('Failed to delete automation:', error);
        alert('Failed to delete automation');
      }
    }
  };

  return (
    <div className={`automation-card ${automation.isActive ? 'active' : 'inactive'}`}>
      <div className="automation-header">
        <h3>{automation.name}</h3>
        <div className="automation-toggle">
          <label className="switch">
            <input
              type="checkbox"
              checked={automation.isActive}
              onChange={handleToggle}
              disabled={loading}
            />
            <span className="slider"></span>
          </label>
        </div>
      </div>

      <div className="automation-details">
        <p><strong>Device:</strong> {automation.device?.name || `Device #${automation.deviceId}`}</p>
        <p><strong>Time:</strong> {automation.scheduleTime}</p>
        <p><strong>Days:</strong> {automation.daysOfWeek || 'Daily'}</p>
        <p><strong>Action:</strong> Turn {automation.action === 0 ? 'ON' : 'OFF'}</p>
        {automation.createdAt && (
          <p className="created-at">
            Created: {format(new Date(automation.createdAt), 'MMM dd, yyyy HH:mm')}
          </p>
        )}
      </div>

      <button className="delete-btn" onClick={handleDelete}>
        Delete
      </button>
    </div>
  );
};

export default AutomationCard;
