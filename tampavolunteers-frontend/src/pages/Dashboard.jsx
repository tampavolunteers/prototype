import React from 'react';
import { useAuth } from '../context/AuthContext';

const Dashboard = () => {
  const { user } = useAuth();

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-6 text-gray-800">Dashboard</h1>

      <div className="bg-white p-6 rounded-lg shadow-md mb-6">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">Welcome, {user?.firstName}!</h2>
        <p className="text-gray-600">Email: {user?.email}</p>
        <p className="text-gray-600">Role: {user?.role}</p>
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
