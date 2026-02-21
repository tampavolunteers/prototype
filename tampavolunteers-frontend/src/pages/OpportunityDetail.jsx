import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { authService } from '../services/authService';

const STATUS_STYLES = {
  PUBLISHED: 'bg-green-100 text-green-800',
  DRAFT: 'bg-gray-100 text-gray-800',
  CANCELLED: 'bg-red-100 text-red-800',
  COMPLETED: 'bg-blue-100 text-blue-800',
};

const OpportunityDetail = () => {
  const { id } = useParams();
  const [opp, setOpp] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    authService.getOpportunity(id)
      .then(setOpp)
      .catch(() => setError('Opportunity not found or failed to load.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto py-12 text-center text-gray-500">
        Loading...
      </div>
    );
  }

  if (error || !opp) {
    return (
      <div className="max-w-3xl mx-auto">
        <Link to="/opportunities" className="text-blue-600 hover:underline text-sm">&larr; Back to Opportunities</Link>
        <div className="mt-6 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {error || 'Opportunity not found.'}
        </div>
      </div>
    );
  }

  const start = new Date(opp.startDateTime);
  const end = new Date(opp.endDateTime);
  const slotsLeft = opp.slotsAvailable - opp.slotsFilled;
  const location = [opp.street, opp.city, opp.state, opp.zip].filter(Boolean).join(', ');

  return (
    <div className="max-w-3xl mx-auto">
      <Link to="/opportunities" className="text-blue-600 hover:underline text-sm">&larr; Back to Opportunities</Link>

      <div className="bg-white rounded-lg shadow-md p-8 mt-4">
        <div className="flex items-start justify-between mb-2">
          <h1 className="text-3xl font-bold text-gray-800 flex-1">{opp.title}</h1>
          <span className={`ml-4 px-3 py-1 rounded-full text-sm font-medium whitespace-nowrap ${STATUS_STYLES[opp.status] || 'bg-gray-100 text-gray-800'}`}>
            {opp.status}
          </span>
        </div>

        <p className="text-blue-700 font-medium mb-1">{opp.organizationName}</p>

        {opp.categoryName && (
          <span className="inline-block px-3 py-1 bg-blue-100 text-blue-700 text-sm font-medium rounded mb-4">
            {opp.categoryName}
          </span>
        )}

        <p className="text-gray-700 leading-relaxed mb-6">{opp.description}</p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 border-t pt-6">
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase mb-1">Start</h3>
            <p className="text-gray-800">
              {start.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}
              <br />
              {start.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>

          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase mb-1">End</h3>
            <p className="text-gray-800">
              {end.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}
              <br />
              {end.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>

          {location && (
            <div>
              <h3 className="text-xs font-semibold text-gray-500 uppercase mb-1">Location</h3>
              <p className="text-gray-800">{location}</p>
            </div>
          )}

          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase mb-1">Slots</h3>
            <p className={slotsLeft > 0 ? 'text-green-700 font-medium' : 'text-red-600 font-medium'}>
              {slotsLeft > 0
                ? `${slotsLeft} of ${opp.slotsAvailable} available`
                : `Full (${opp.slotsAvailable} slots)`}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OpportunityDetail;
