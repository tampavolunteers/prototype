import React from 'react';
import { useAuth } from '../context/AuthContext';

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
  const { user } = useAuth();
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
      </div>
    </div>
  );
};

export default Dashboard;
