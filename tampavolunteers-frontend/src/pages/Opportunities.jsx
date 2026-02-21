import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '../services/authService';

const Opportunities = () => {
  const [opportunities, setOpportunities] = useState([]);
  const [categories, setCategories] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [organizationId, setOrganizationId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  useEffect(() => {
    authService.getCategories().then(setCategories).catch(() => {});
    authService.getOrganizations().then(data => {
      setOrganizations(Array.isArray(data) ? data : data.content || []);
    }).catch(() => {});
  }, []);

  const fetchOpportunities = useCallback(async (p = 0) => {
    setLoading(true);
    setError(null);
    try {
      const params = { page: p, size: 12 };
      if (submittedKeyword) params.keyword = submittedKeyword;
      if (categoryId) params.categoryId = categoryId;
      if (organizationId) params.organizationId = organizationId;
      if (startDate) params.startDate = new Date(startDate).toISOString().slice(0, 19);
      if (endDate) params.endDate = new Date(endDate).toISOString().slice(0, 19);

      const data = await authService.getOpportunities(params);
      setOpportunities(data.content || []);
      setTotalPages(data.totalPages || 0);
      setPage(p);
    } catch (err) {
      setError('Failed to load opportunities. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [submittedKeyword, categoryId, organizationId, startDate, endDate]);

  useEffect(() => {
    fetchOpportunities(0);
  }, [fetchOpportunities]);

  const handleSearch = (e) => {
    e.preventDefault();
    setSubmittedKeyword(keyword);
  };

  const handleReset = () => {
    setKeyword('');
    setSubmittedKeyword('');
    setCategoryId('');
    setOrganizationId('');
    setStartDate('');
    setEndDate('');
  };

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-6 text-gray-800">Volunteer Opportunities</h1>

      <div className="bg-white p-6 rounded-lg shadow-md mb-6">
        <h2 className="text-lg font-semibold mb-4 text-gray-700">Filter Opportunities</h2>
        <form onSubmit={handleSearch}>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-4">
            <div className="lg:col-span-3 md:col-span-2 flex gap-2">
              <input
                type="text"
                placeholder="Search by keyword..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              />
              <button
                type="submit"
                className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition"
              >
                Search
              </button>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Category</label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              >
                <option value="">All Categories</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Organization</label>
              <select
                value={organizationId}
                onChange={(e) => setOrganizationId(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              >
                <option value="">All Organizations</option>
                {organizations.map((o) => (
                  <option key={o.id} value={o.id}>{o.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Start Date</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">End Date</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          <button
            type="button"
            onClick={handleReset}
            className="text-sm text-gray-500 hover:text-gray-700 underline"
          >
            Reset filters
          </button>
        </form>
      </div>

      {loading && (
        <div className="flex justify-center py-12">
          <div className="text-gray-500">Loading opportunities...</div>
        </div>
      )}

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
          {error}
        </div>
      )}

      {!loading && !error && opportunities.length === 0 && (
        <div className="bg-white p-12 rounded-lg shadow-md text-center text-gray-500">
          No opportunities found. Try adjusting your filters.
        </div>
      )}

      {!loading && !error && opportunities.length > 0 && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
            {opportunities.map((opp) => (
              <OpportunityCard key={opp.id} opp={opp} />
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex justify-center space-x-2">
              <button
                onClick={() => fetchOpportunities(page - 1)}
                disabled={page === 0}
                className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50 hover:bg-gray-300 transition"
              >
                Previous
              </button>
              <span className="px-4 py-2 text-gray-600">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => fetchOpportunities(page + 1)}
                disabled={page >= totalPages - 1}
                className="px-4 py-2 bg-gray-200 rounded disabled:opacity-50 hover:bg-gray-300 transition"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

const OpportunityCard = ({ opp }) => {
  const start = new Date(opp.startDateTime);
  const slotsLeft = opp.slotsAvailable - opp.slotsFilled;

  return (
    <div className="bg-white rounded-lg shadow-md p-5 flex flex-col hover:shadow-lg transition">
      <div className="flex items-start justify-between mb-2">
        <h3 className="text-lg font-semibold text-gray-800 leading-tight flex-1">{opp.title}</h3>
        {opp.categoryName && (
          <span className="ml-2 px-2 py-1 bg-blue-100 text-blue-700 text-xs font-medium rounded whitespace-nowrap">
            {opp.categoryName}
          </span>
        )}
      </div>

      <p className="text-sm text-gray-500 mb-1">{opp.organizationName}</p>

      <p className="text-sm text-gray-600 mb-3 flex-1 line-clamp-2">{opp.description}</p>

      <div className="text-sm text-gray-600 space-y-1 mb-4">
        <div>
          {start.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })}
          {' '}at{' '}
          {start.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
        </div>
        {(opp.city || opp.state) && (
          <div>{[opp.city, opp.state].filter(Boolean).join(', ')}</div>
        )}
        <div className={slotsLeft > 0 ? 'text-green-700' : 'text-red-600'}>
          {slotsLeft > 0 ? `${slotsLeft} slot${slotsLeft !== 1 ? 's' : ''} available` : 'Full'}
        </div>
      </div>

      <Link
        to={`/opportunities/${opp.id}`}
        className="mt-auto text-center bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition text-sm font-medium"
      >
        View Details
      </Link>
    </div>
  );
};

export default Opportunities;
