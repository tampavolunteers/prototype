import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';

const ROLE_HIERARCHY = ['VOLUNTEER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN'];

const ROLE_COLORS = {
  SUPER_ADMIN: 'bg-purple-100 text-purple-800',
  ADMIN:       'bg-red-100 text-red-800',
  ORG_ADMIN:   'bg-yellow-100 text-yellow-800',
  VOLUNTEER:   'bg-green-100 text-green-800',
};

const STATUS_LABELS = {
  VOLUNTEER:           'Volunteer',
  ORG_REPRESENTATIVE:  'Organization Representative',
  BOTH:                'Volunteer & Org Representative',
  INACTIVE:            'Inactive',
};

// Returns all roles the user holds, highest first (e.g. SUPER_ADMIN → [SUPER_ADMIN, ADMIN, ORG_ADMIN, VOLUNTEER])
function getCumulativeRoles(role) {
  const idx = ROLE_HIERARCHY.indexOf(role);
  if (idx < 0) return [role];
  return ROLE_HIERARCHY.slice(0, idx + 1).reverse();
}

const Dashboard = () => {
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [showOrgForm, setShowOrgForm] = useState(false);
  const roles = getCumulativeRoles(user?.role);

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-6 text-gray-800">Dashboard</h1>

      <div className="bg-white p-6 rounded-lg shadow-md mb-6">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">Welcome, {user?.firstName}!</h2>
        <p className="text-gray-600 mb-3">Email: {user?.email}</p>
        <div className="mb-3">
          <p className="text-sm text-gray-500 mb-1">Roles</p>
          <div className="flex flex-wrap gap-2">
            {roles.map(role => (
              <span
                key={role}
                className={`px-2 py-1 rounded text-xs font-semibold ${ROLE_COLORS[role] ?? 'bg-gray-100 text-gray-800'}`}
              >
                {role}
              </span>
            ))}
          </div>
        </div>
        {user?.userStatus && (
          <div>
            <p className="text-sm text-gray-500 mb-1">Volunteer Status</p>
            <span className="px-2 py-1 rounded text-xs font-semibold bg-blue-100 text-blue-800">
              {STATUS_LABELS[user.userStatus] ?? user.userStatus}
            </span>
          </div>
        )}
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-xl font-bold mb-3 text-gray-800">My Registrations</h3>
          <p className="text-gray-600">View your upcoming volunteer opportunities here.</p>
          <button className="mt-4 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition">
            View Registrations
          </button>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-xl font-bold mb-3 text-gray-800">Volunteer Hours</h3>
          <p className="text-gray-600">Track your volunteer hours and impact.</p>
          <button className="mt-4 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition">
            View Hours
          </button>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-md md:col-span-2">
          <h3 className="text-xl font-bold mb-3 text-gray-800">Register Your Organization</h3>
          <p className="text-gray-600">
            Represent a nonprofit or volunteer organization? Register it here to post opportunities and manage volunteers.
          </p>
          <button
            onClick={() => setShowOrgForm(true)}
            className="mt-4 bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700 transition"
          >
            Register Organization
          </button>
        </div>
      </div>

      {showOrgForm && (
        <CreateOrgModal
          onClose={() => setShowOrgForm(false)}
          onCreated={async () => {
            await refreshUser();
            navigate('/org-dashboard');
          }}
        />
      )}
    </div>
  );
};

const EMPTY_FORM = {
  name: '', description: '', website: '', contactEmail: '',
  contactPhone: '', street: '', city: '', state: '', zip: '',
};

const CreateOrgModal = ({ onClose, onCreated }) => {
  const { hasRole } = useAuth();
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await authService.createOrganization(form);
      onCreated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create organization.');
      setSubmitting(false);
    }
  };

  const isSuperAdmin = hasRole('SUPER_ADMIN');

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-screen flex flex-col">
        <div className="p-6 border-b flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-800">Register Organization</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto flex-1 space-y-4">
          {!isSuperAdmin && (
            <div className="bg-yellow-50 border border-yellow-200 text-yellow-800 px-3 py-2 rounded text-sm">
              Your organization will be listed as pending until reviewed and approved by a platform administrator.
            </div>
          )}

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-3 py-2 rounded text-sm">{error}</div>
          )}

          <div className="grid grid-cols-1 gap-4">
            <Field label="Organization Name *" value={form.name} onChange={set('name')} required />
            <Field label="Description" value={form.description} onChange={set('description')} textarea />
            <Field label="Website" value={form.website} onChange={set('website')} type="url" />
            <Field label="Contact Email *" value={form.contactEmail} onChange={set('contactEmail')} type="email" required />
            <Field label="Contact Phone" value={form.contactPhone} onChange={set('contactPhone')} />
            <Field label="Street" value={form.street} onChange={set('street')} />
            <div className="grid grid-cols-3 gap-3">
              <div className="col-span-2">
                <Field label="City" value={form.city} onChange={set('city')} />
              </div>
              <Field label="State" value={form.state} onChange={set('state')} maxLength={2} placeholder="FL" />
            </div>
            <Field label="ZIP" value={form.zip} onChange={set('zip')} maxLength={10} />
          </div>
        </form>

        <div className="p-6 border-t flex justify-end gap-3">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="px-4 py-2 bg-indigo-600 text-white text-sm rounded hover:bg-indigo-700 disabled:bg-gray-400 transition"
          >
            {submitting ? 'Submitting...' : 'Submit'}
          </button>
        </div>
      </div>
    </div>
  );
};

const Field = ({ label, value, onChange, textarea, required, ...props }) => (
  <div>
    <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
    {textarea ? (
      <textarea
        value={value}
        onChange={onChange}
        rows={3}
        className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"
      />
    ) : (
      <input
        type={props.type || 'text'}
        value={value}
        onChange={onChange}
        required={required}
        className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"
        {...props}
      />
    )}
  </div>
);

export default Dashboard;
