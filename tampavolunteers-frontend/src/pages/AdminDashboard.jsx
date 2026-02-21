import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import api from '../services/api';

const TABS = ['Users', 'Organizations', 'Audit Log'];

const AdminDashboard = () => {
  const { user, hasRole } = useAuth();
  const [activeTab, setActiveTab] = useState('Users');

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 text-gray-800">Admin Dashboard</h1>

      <div className="flex border-b mb-6">
        {TABS.filter(tab => tab !== 'Audit Log' || hasRole('SUPER_ADMIN')).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-6 py-3 font-medium transition ${
              activeTab === tab
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {activeTab === 'Users' && <UsersTab hasRole={hasRole} />}
      {activeTab === 'Organizations' && <OrganizationsTab />}
      {activeTab === 'Audit Log' && hasRole('SUPER_ADMIN') && <AuditLogTab />}
    </div>
  );
};

const UsersTab = ({ hasRole }) => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [editingUser, setEditingUser] = useState(null);

  const fetchUsers = async (p = 0) => {
    setLoading(true);
    try {
      const data = await authService.getAdminUsers(p, 20);
      setUsers(data.content || []);
      setTotalPages(data.totalPages || 0);
      setPage(p);
    } catch (err) {
      setError('Failed to load users.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchUsers(); }, []);

  if (loading) return <div className="py-4 text-gray-600">Loading users...</div>;

  return (
    <div>
      {editingUser && (
        <UserEditModal
          user={editingUser}
          onClose={() => setEditingUser(null)}
          onSave={(msg) => {
            setEditingUser(null);
            setMessage(msg);
            fetchUsers(page);
          }}
          onError={(msg) => setError(msg)}
        />
      )}

      {message && (
        <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-2 rounded mb-4">
          {message}
        </div>
      )}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-2 rounded mb-4">
          {error}
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Last Login</th>
              {hasRole('SUPER_ADMIN') && (
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
              )}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {users.map((u) => (
              <tr key={u.id}>
                <td className="px-4 py-3 text-sm text-gray-600">{u.id}</td>
                <td className="px-4 py-3 text-sm text-gray-900">{u.email}</td>
                <td className="px-4 py-3 text-sm text-gray-900">{u.firstName} {u.lastName}</td>
                <td className="px-4 py-3 text-sm">
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    u.role === 'SUPER_ADMIN' ? 'bg-purple-100 text-purple-800' :
                    u.role === 'ADMIN' ? 'bg-red-100 text-red-800' :
                    u.role === 'ORG_ADMIN' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-green-100 text-green-800'
                  }`}>
                    {u.role}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-600">
                  {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString() : 'Never'}
                </td>
                {hasRole('SUPER_ADMIN') && (
                  <td className="px-4 py-3 text-sm">
                    <button
                      onClick={() => setEditingUser(u)}
                      className="text-blue-600 hover:text-blue-800 text-xs font-medium underline"
                    >
                      Edit
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center mt-4 space-x-2">
          <button
            onClick={() => fetchUsers(page - 1)}
            disabled={page === 0}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-4 py-2 text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => fetchUsers(page + 1)}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

const ROLES = ['VOLUNTEER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN'];

const UserEditModal = ({ user, onClose, onSave, onError }) => {
  const [role, setRole] = useState(user.role);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await api.put(`/super-admin/users/${user.id}/role`, { role });
      onSave(`Role updated to ${role} for ${user.email}.`);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update role.';
      setError(msg);
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-6 w-96 max-w-full mx-4">
        <h3 className="text-lg font-bold mb-1 text-gray-800">Edit User</h3>
        <p className="text-sm text-gray-500 mb-1">{user.firstName} {user.lastName}</p>
        <p className="text-xs text-gray-400 mb-4">{user.email}</p>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-3 py-2 rounded mb-3 text-sm">
            {error}
          </div>
        )}

        <div className="mb-5">
          <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
          >
            {ROLES.map(r => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        </div>

        <div className="flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving || role === user.role}
            className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:bg-gray-400 transition"
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
};

const OrganizationsTab = () => {
  const [orgs, setOrgs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchOrgs = async (p = 0) => {
    setLoading(true);
    try {
      const data = await authService.getAdminOrganizations(p, 20);
      setOrgs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setPage(p);
    } catch (err) {
      setError('Failed to load organizations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchOrgs(); }, []);

  const handleVerify = async (id) => {
    try {
      await authService.verifyOrganization(id);
      setMessage('Organization verified.');
      fetchOrgs(page);
    } catch (err) {
      setError('Failed to verify organization.');
    }
  };

  if (loading) return <div className="py-4 text-gray-600">Loading organizations...</div>;

  return (
    <div>
      {message && (
        <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-2 rounded mb-4">
          {message}
        </div>
      )}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-2 rounded mb-4">
          {error}
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Owner</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Location</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {orgs.map((org) => (
              <tr key={org.id}>
                <td className="px-4 py-3 text-sm text-gray-600">{org.id}</td>
                <td className="px-4 py-3 text-sm">
                  <div className="font-medium text-gray-900">{org.name}</div>
                  {org.seeded && (
                    <span className="text-xs text-gray-400 font-mono">seeded</span>
                  )}
                </td>
                <td className="px-4 py-3 text-sm text-gray-600">{org.ownerEmail}</td>
                <td className="px-4 py-3 text-sm text-gray-600">
                  {[org.city, org.state].filter(Boolean).join(', ') || '—'}
                </td>
                <td className="px-4 py-3 text-sm">
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    org.verified ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                  }`}>
                    {org.verified ? 'Verified' : 'Pending'}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm">
                  {!org.verified && (
                    <button
                      onClick={() => handleVerify(org.id)}
                      className="bg-green-600 text-white px-3 py-1 rounded text-xs hover:bg-green-700 transition"
                    >
                      Verify
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {orgs.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
                  No organizations found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center mt-4 space-x-2">
          <button
            onClick={() => fetchOrgs(page - 1)}
            disabled={page === 0}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-4 py-2 text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => fetchOrgs(page + 1)}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

const AuditLogTab = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchLogs = async (p = 0) => {
    setLoading(true);
    try {
      const response = await api.get('/super-admin/audit-log', { params: { page: p, size: 20 } });
      setLogs(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setPage(p);
    } catch (err) {
      // silently fail
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchLogs(); }, []);

  if (loading) return <div className="py-4 text-gray-600">Loading audit log...</div>;

  return (
    <div>
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Admin</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Target</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Details</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {logs.map((log) => (
              <tr key={log.id}>
                <td className="px-4 py-3 text-xs text-gray-600">
                  {new Date(log.createdAt).toLocaleString()}
                </td>
                <td className="px-4 py-3 text-sm text-gray-900">{log.adminEmail}</td>
                <td className="px-4 py-3 text-sm font-medium text-blue-700">{log.action}</td>
                <td className="px-4 py-3 text-sm text-gray-600">
                  {log.targetType} #{log.targetId}
                </td>
                <td className="px-4 py-3 text-xs text-gray-500 font-mono">{log.details}</td>
              </tr>
            ))}
            {logs.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-gray-500">
                  No audit log entries found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center mt-4 space-x-2">
          <button
            onClick={() => fetchLogs(page - 1)}
            disabled={page === 0}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-4 py-2 text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => fetchLogs(page + 1)}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
