import React from 'react';

const Opportunities = () => {
  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-4xl font-bold mb-6 text-gray-800">Volunteer Opportunities</h1>

      <div className="bg-white p-6 rounded-lg shadow-md mb-6">
        <h2 className="text-xl font-semibold mb-4 text-gray-800">Search Opportunities</h2>
        <div className="flex gap-4">
          <input
            type="text"
            placeholder="Search by keyword..."
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-blue-500"
          />
          <button className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition">
            Search
          </button>
        </div>
      </div>

      <div className="bg-white p-6 rounded-lg shadow-md">
        <p className="text-gray-600 text-center">
          No opportunities available at the moment. Check back soon!
        </p>
      </div>
    </div>
  );
};

export default Opportunities;
