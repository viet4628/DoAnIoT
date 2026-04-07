import React, { useState, useEffect } from 'react';
import { automationApi, deviceApi } from '../services/api';
import AutomationCard from '../components/AutomationCard';
import '../styles/Automations.css';

const Automations = () => {
  const [automations, setAutomations] = useState([]);
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    deviceId: '',
    scheduleTime: '',
    daysOfWeek: '',
    action: '0',
    isActive: true,
  });

  const fetchAutomations = async () => {
    try {
      const response = await automationApi.getAutomations();
      setAutomations(response.data);
    } catch (error) {
      console.error('Failed to fetch automations:', error);
    }
  };

  const fetchDevices = async () => {
    try {
      const response = await deviceApi.getDevices();
      setDevices(response.data);
    } catch (error) {
      console.error('Failed to fetch devices:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAutomations();
    fetchDevices();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await automationApi.createAutomation({
        ...formData,
        deviceId: parseInt(formData.deviceId),
        action: parseInt(formData.action),
      });
      setFormData({
        name: '',
        deviceId: '',
        scheduleTime: '',
        daysOfWeek: '',
        action: '0',
        isActive: true,
      });
      setShowForm(false);
      fetchAutomations();
    } catch (error) {
      console.error('Failed to create automation:', error);
      alert('Failed to create automation');
    }
  };

  const handleDelete = (id) => {
    setAutomations(automations.filter(a => a.id !== id));
  };

  if (loading) {
    return <div className="automations"><div className="loading">Loading...</div></div>;
  }

  return (
    <div className="automations">
      <div className="automations-header">
        <h1>Automation Schedules</h1>
        <button className="add-btn" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : '+ New Automation'}
        </button>
      </div>

      {showForm && (
        <div className="automation-form-container">
          <h2>Create New Automation</h2>
          <form onSubmit={handleSubmit} className="automation-form">
            <div className="form-group">
              <label>Name</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label>Device</label>
              <select
                value={formData.deviceId}
                onChange={(e) => setFormData({ ...formData, deviceId: e.target.value })}
                required
              >
                <option value="">Select Device</option>
                {devices.map((device) => (
                  <option key={device.id} value={device.id}>
                    {device.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Time (HH:MM)</label>
              <input
                type="time"
                value={formData.scheduleTime}
                onChange={(e) => setFormData({ ...formData, scheduleTime: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label>Days of Week (comma-separated, e.g., Mon,Wed,Fri)</label>
              <input
                type="text"
                value={formData.daysOfWeek}
                onChange={(e) => setFormData({ ...formData, daysOfWeek: e.target.value })}
                placeholder="Leave empty for daily"
              />
            </div>

            <div className="form-group">
              <label>Action</label>
              <select
                value={formData.action}
                onChange={(e) => setFormData({ ...formData, action: e.target.value })}
              >
                <option value="0">Turn ON</option>
                <option value="1">Turn OFF</option>
              </select>
            </div>

            <button type="submit" className="submit-btn">
              Create Automation
            </button>
          </form>
        </div>
      )}

      <div className="automations-grid">
        {automations.length === 0 ? (
          <p className="no-automations">No automations configured yet.</p>
        ) : (
          automations.map((automation) => (
            <AutomationCard
              key={automation.id}
              automation={automation}
              onUpdate={fetchAutomations}
              onDelete={handleDelete}
            />
          ))
        )}
      </div>
    </div>
  );
};

export default Automations;
