import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { authService } from '../services/authService';

const USER_STATUS_OPTIONS = [
  { value: 'VOLUNTEER', label: 'Volunteer' },
  { value: 'ORG_REPRESENTATIVE', label: 'Organization Representative' },
  { value: 'BOTH', label: 'Both' },
  { value: 'INACTIVE', label: 'Inactive' },
];

const ProfileSettings = () => {
  const { user, setUserData } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    bio: '',
    avatarUrl: '',
    phone: '',
  });

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get('/users/me');
        const data = response.data;
        setProfile(data);
        setFormData({
          firstName: data.firstName || '',
          lastName: data.lastName || '',
          bio: data.bio || '',
          avatarUrl: data.avatarUrl || '',
          phone: data.phone || '',
        });
      } catch (err) {
        setError('Failed to load profile.');
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, []);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const response = await api.put('/users/me', formData);
      setProfile(response.data);
      const stored = authService.getStoredUser();
      const updated = { ...stored, firstName: response.data.firstName, lastName: response.data.lastName };
      localStorage.setItem('user', JSON.stringify(updated));
      setUserData(updated);
      setMessage('Profile updated successfully.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (e) => {
    const userStatus = e.target.value;
    try {
      const response = await api.put('/users/me/status', { userStatus });
      setProfile(response.data);
      const stored = authService.getStoredUser();
      const updated = { ...stored, userStatus: response.data.userStatus };
      localStorage.setItem('user', JSON.stringify(updated));
      setUserData(updated);
      setMessage('Status updated.');
    } catch (err) {
      setError('Failed to update status.');
    }
  };

  const handleVisibilityToggle = async () => {
    const newValue = !profile.isPublic;
    try {
      const response = await api.put('/users/me/visibility', { isPublic: newValue });
      setProfile(response.data);
      setMessage(newValue ? 'Profile is now public.' : 'Profile is now private.');
    } catch (err) {
      setError('Failed to update visibility.');
    }
  };

  if (loading) {
    return <div className="flex justify-center py-8">Loading profile...</div>;
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 text-gray-800">Profile Settings</h1>

      {message && (
        <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
          {message}
        </div>
      )}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      {/* Profile Form */}
      <div className="bg-white p-6 rounded-lg shadow-md mb-6">
        <h2 className="text-xl font-bold mb-4 text-gray-800">Basic Information</h2>
        <form onSubmit={handleSaveProfile}>
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">First Name</label>
              <input
                type="text"
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">Last Name</label>
              <input
                type="text"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Phone</label>
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Bio</label>
            <textarea
              name="bio"
              value={formData.bio}
              onChange={handleChange}
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              placeholder="Tell us about yourself..."
            />
          </div>

          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Avatar URL</label>
            <input
              type="url"
              name="avatarUrl"
              value={formData.avatarUrl}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              placeholder="https://..."
            />
          </div>

          <button
            type="submit"
            disabled={saving}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition disabled:bg-gray-400"
          >
            {saving ? 'Saving...' : 'Save Profile'}
          </button>
        </form>
      </div>

      {/* Status */}
      <div className="bg-white p-6 rounded-lg shadow-md mb-6">
        <h2 className="text-xl font-bold mb-4 text-gray-800">Volunteer Status</h2>
        <select
          value={profile?.userStatus || 'VOLUNTEER'}
          onChange={handleStatusChange}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
        >
          {USER_STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      {/* Visibility */}
      <div className="bg-white p-6 rounded-lg shadow-md">
        <h2 className="text-xl font-bold mb-4 text-gray-800">Profile Visibility</h2>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-gray-700 font-medium">Public Profile</p>
            <p className="text-gray-500 text-sm">
              Allow other users to find and view your profile.
            </p>
          </div>
          <button
            onClick={handleVisibilityToggle}
            className={`px-4 py-2 rounded-lg font-medium transition ${
              profile?.isPublic
                ? 'bg-green-500 hover:bg-green-600 text-white'
                : 'bg-gray-200 hover:bg-gray-300 text-gray-700'
            }`}
          >
            {profile?.isPublic ? 'Public' : 'Private'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProfileSettings;
