import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';

const OrgDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [org, setOrg] = useState(null);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteInput, setDeleteInput] = useState('');
  const [deleting, setDeleting] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      const [myOrgs, memberData] = await Promise.all([
        authService.getMyOrganizations(),
        authService.getOrgMembers(id),
      ]);
      const found = myOrgs.find((o) => String(o.id) === String(id));
      if (!found) {
        setError('Organization not found or you do not have access.');
        setLoading(false);
        return;
      }
      setOrg(found);
      setMembers(memberData);
    } catch (err) {
      setError('Failed to load organization details.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleRoleChange = async (userId, role) => {
    try {
      await authService.updateOrgMemberRole(id, userId, role);
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update role.');
    }
  };

  const handleRemove = async (userId) => {
    try {
      await authService.removeOrgMember(id, userId);
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to remove member.');
    }
  };

  const handleDelete = async () => {
    if (deleteInput !== org.name) return;
    setDeleting(true);
    try {
      await authService.deleteOrganization(id);
      navigate('/org-dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete organization.');
      setDeleting(false);
    }
  };

  if (loading) return <div className="py-8 text-gray-600">Loading...</div>;
  if (error && !org) return <div className="py-8 text-red-600">{error}</div>;

  const isOwner = org.myRole === 'OWNER';

  return (
    <div className="max-w-4xl mx-auto">
      <Link to="/org-dashboard" className="text-blue-600 hover:text-blue-800 text-sm mb-6 inline-block">
        ← My Organizations
      </Link>

      <div className="mb-6">
        <div className="flex items-center gap-3">
          <h1 className="text-3xl font-bold text-gray-800">{org.name}</h1>
          {org.verified && (
            <span className="px-2 py-0.5 rounded text-sm font-medium bg-green-100 text-green-800">Verified</span>
          )}
        </div>
        <p className="text-gray-500 mt-1">
          {[org.city, org.state].filter(Boolean).join(', ')}
          {org.contactEmail && ` · ${org.contactEmail}`}
        </p>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-2 rounded mb-4">{error}</div>
      )}

      <div className="bg-white rounded-lg shadow border border-gray-200 p-6 mb-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold text-gray-800">Members</h2>
          <span className="text-sm text-gray-500">Your role: <strong>{org.myRole}</strong></span>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Org Role</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Joined</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {members.map((m) => {
                const isMemberOwner = m.role === 'OWNER';
                const canChangeRole = isOwner && !isMemberOwner;
                const canRemove = !isMemberOwner && (isOwner || m.role === 'MEMBER');
                return (
                  <tr key={m.id}>
                    <td className="px-4 py-3 text-gray-900">{m.firstName} {m.lastName}</td>
                    <td className="px-4 py-3 text-gray-600">{m.email}</td>
                    <td className="px-4 py-3">
                      {canChangeRole ? (
                        <select
                          value={m.role}
                          onChange={(e) => handleRoleChange(m.userId, e.target.value)}
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
                    <td className="px-4 py-3 text-gray-600">
                      {m.joinedAt ? new Date(m.joinedAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 py-3">
                      {canRemove ? (
                        <button
                          onClick={() => handleRemove(m.userId)}
                          className="text-red-600 hover:text-red-800 text-sm font-medium"
                        >
                          Remove
                        </button>
                      ) : (
                        <span className="text-gray-400 text-sm">—</span>
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
      </div>

      {isOwner && (
        <div className="mt-4">
          {!confirmDelete ? (
            <button
              onClick={() => setConfirmDelete(true)}
              className="text-red-600 border border-red-300 px-4 py-2 rounded text-sm hover:bg-red-50 transition"
            >
              ⚠ Delete Organization
            </button>
          ) : (
            <div className="bg-red-50 border border-red-200 rounded p-4 space-y-3">
              <p className="text-sm text-red-700">
                This action is irreversible. Type <strong>{org.name}</strong> to confirm:
              </p>
              <input
                type="text"
                value={deleteInput}
                onChange={(e) => setDeleteInput(e.target.value)}
                className="border border-gray-300 rounded px-3 py-2 text-sm w-full max-w-sm"
                placeholder={org.name}
              />
              <div className="flex gap-3">
                <button
                  onClick={handleDelete}
                  disabled={deleteInput !== org.name || deleting}
                  className="bg-red-600 text-white px-4 py-2 rounded text-sm hover:bg-red-700 disabled:bg-gray-400 transition"
                >
                  {deleting ? 'Deleting...' : 'Confirm Delete'}
                </button>
                <button
                  onClick={() => { setConfirmDelete(false); setDeleteInput(''); }}
                  className="text-gray-600 px-4 py-2 text-sm hover:text-gray-800"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default OrgDetail;
