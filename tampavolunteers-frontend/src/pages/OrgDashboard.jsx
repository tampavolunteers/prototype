import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '../services/authService';

const OrgDashboard = () => {
  const [orgs, setOrgs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchOrgs = useCallback(async () => {
    try {
      const data = await authService.getMyOrganizations();
      setOrgs(data);
    } catch (err) {
      setError('Failed to load your organizations.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchOrgs(); }, [fetchOrgs]);

  if (loading) return <div className="py-8 text-gray-600">Loading your organizations...</div>;

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold mb-6 text-gray-800">My Organizations</h1>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-2 rounded mb-4">{error}</div>
      )}

      {orgs.length === 0 && (
        <div className="text-gray-500 py-8 text-center">You have no organizations yet.</div>
      )}

      <div className="space-y-4">
        {orgs.map((org) => (
          <OrgCard key={org.id} org={org} onRefresh={fetchOrgs} />
        ))}
      </div>
    </div>
  );
};

const OrgCard = ({ org, onRefresh }) => {
  const [expanded, setExpanded] = useState(false);
  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [error, setError] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteInput, setDeleteInput] = useState('');
  const [deleting, setDeleting] = useState(false);

  const isOwner = org.myRole === 'OWNER';

  const fetchMembers = useCallback(async () => {
    setMembersLoading(true);
    try {
      const data = await authService.getOrgMembers(org.id);
      setMembers(data);
    } catch (err) {
      setError('Failed to load members.');
    } finally {
      setMembersLoading(false);
    }
  }, [org.id]);

  const handleToggle = () => {
    if (!expanded) fetchMembers();
    setExpanded(!expanded);
    setError(null);
  };

  const handleRoleChange = async (userId, role) => {
    try {
      await authService.updateOrgMemberRole(org.id, userId, role);
      fetchMembers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update role.');
    }
  };

  const handleRemove = async (userId) => {
    try {
      await authService.removeOrgMember(org.id, userId);
      fetchMembers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to remove member.');
    }
  };

  const handleDelete = async () => {
    if (deleteInput !== org.name) return;
    setDeleting(true);
    try {
      await authService.deleteOrganization(org.id);
      onRefresh();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete organization.');
      setDeleting(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow border border-gray-200">
      <div className="p-5 flex justify-between items-start">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold text-gray-900">{org.name}</h2>
            {org.verified ? (
              <span className="px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">Verified</span>
            ) : (
              <span className="px-2 py-0.5 rounded text-xs font-medium bg-yellow-100 text-yellow-800">Pending Approval</span>
            )}
            <span className="px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">{org.myRole}</span>
          </div>
          <p className="text-sm text-gray-500 mt-1">
            {[org.city, org.state].filter(Boolean).join(', ')}
            {org.contactEmail && ` · ${org.contactEmail}`}
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to={`/org-dashboard/${org.id}`}
            className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded text-sm hover:bg-gray-200 transition"
          >
            View
          </Link>
          <button
            onClick={handleToggle}
            className="px-3 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 transition"
          >
            Manage {expanded ? '▲' : '▾'}
          </button>
        </div>
      </div>

      {expanded && (
        <div className="border-t border-gray-100 p-5">
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-3 py-2 rounded mb-3 text-sm">{error}</div>
          )}

          {membersLoading ? (
            <div className="text-sm text-gray-600">Loading members...</div>
          ) : (
            <MemberTable
              members={members}
              myRole={org.myRole}
              orgId={org.id}
              onRoleChange={handleRoleChange}
              onRemove={handleRemove}
            />
          )}

          {isOwner && (
            <div className="mt-5 pt-4 border-t border-gray-100">
              {!confirmDelete ? (
                <button
                  onClick={() => setConfirmDelete(true)}
                  className="text-red-600 border border-red-300 px-3 py-1.5 rounded text-sm hover:bg-red-50 transition"
                >
                  ⚠ Delete Organization
                </button>
              ) : (
                <div className="space-y-2">
                  <p className="text-sm text-red-700">
                    Type <strong>{org.name}</strong> to confirm deletion:
                  </p>
                  <input
                    type="text"
                    value={deleteInput}
                    onChange={(e) => setDeleteInput(e.target.value)}
                    className="border border-gray-300 rounded px-3 py-1.5 text-sm w-full max-w-xs"
                    placeholder={org.name}
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={handleDelete}
                      disabled={deleteInput !== org.name || deleting}
                      className="bg-red-600 text-white px-4 py-1.5 rounded text-sm hover:bg-red-700 disabled:bg-gray-400 transition"
                    >
                      {deleting ? 'Deleting...' : 'Confirm Delete'}
                    </button>
                    <button
                      onClick={() => { setConfirmDelete(false); setDeleteInput(''); }}
                      className="text-gray-600 px-4 py-1.5 text-sm hover:text-gray-800"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

const MemberTable = ({ members, myRole, onRoleChange, onRemove }) => {
  const isOwner = myRole === 'OWNER';

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Joined</th>
            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {members.map((m) => {
            const isMemberOwner = m.role === 'OWNER';
            // OWNER can change ADMIN<->MEMBER; ADMIN cannot change roles
            const canChangeRole = isOwner && !isMemberOwner;
            // OWNER can remove ADMIN/MEMBER; ADMIN can remove MEMBER
            const canRemove = !isMemberOwner && (isOwner || m.role === 'MEMBER');
            return (
              <tr key={m.id}>
                <td className="px-4 py-2 text-gray-900">{m.firstName} {m.lastName}</td>
                <td className="px-4 py-2 text-gray-600">{m.email}</td>
                <td className="px-4 py-2">
                  {canChangeRole ? (
                    <select
                      value={m.role}
                      onChange={(e) => onRoleChange(m.userId, e.target.value)}
                      className="border border-gray-300 rounded px-2 py-1 text-xs"
                    >
                      <option value="ADMIN">ADMIN</option>
                      <option value="MEMBER">MEMBER</option>
                    </select>
                  ) : (
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                      isMemberOwner ? 'bg-purple-100 text-purple-800' :
                      m.role === 'ADMIN' ? 'bg-yellow-100 text-yellow-800' :
                      'bg-gray-100 text-gray-700'
                    }`}>
                      {m.role}
                    </span>
                  )}
                </td>
                <td className="px-4 py-2 text-gray-600">
                  {m.joinedAt ? new Date(m.joinedAt).toLocaleDateString() : '—'}
                </td>
                <td className="px-4 py-2">
                  {canRemove ? (
                    <button
                      onClick={() => onRemove(m.userId)}
                      className="text-red-600 hover:text-red-800 text-xs font-medium"
                    >
                      ×
                    </button>
                  ) : (
                    <span className="text-gray-400 text-xs">—</span>
                  )}
                </td>
              </tr>
            );
          })}
          {members.length === 0 && (
            <tr>
              <td colSpan={5} className="px-4 py-6 text-center text-gray-500">No members found.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default OrgDashboard;
