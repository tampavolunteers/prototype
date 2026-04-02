import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Home = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="w-full mx-auto">
      <div className="text-center py-12">
        <h1 className="text-5xl font-bold text-gray-800 mb-6">
          Welcome to Tampa Volunteers
        </h1>
        <p className="text-xl text-gray-600 mb-8">
          Connecting community members with local volunteer opportunities in the Tampa Bay area
        </p>

        <div className="flex justify-center space-x-4">
          {!isAuthenticated && (
            <>
              <Link
                to="/register"
                className="bg-blue-600 text-white px-8 py-3 rounded-lg text-lg font-semibold hover:bg-blue-700 transition"
              >
                Get Started
              </Link>
              <Link
                to="/login"
                className="bg-gray-200 text-gray-800 px-8 py-3 rounded-lg text-lg font-semibold hover:bg-gray-300 transition"
              >
                Login
              </Link>
            </>
          )}
          <Link
            to="/opportunities"
            className="bg-green-600 text-white px-8 py-3 rounded-lg text-lg font-semibold hover:bg-green-700 transition"
          >
            Browse Opportunities
          </Link>
        </div>
      </div>

      <div className="grid md:grid-cols-3 gap-8 mt-16">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-xl font-bold mb-3 text-gray-800">For Volunteers</h3>
          <p className="text-gray-600">
            Find meaningful volunteer opportunities that match your interests and skills in the Tampa Bay area.
          </p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-xl font-bold mb-3 text-gray-800">For Organizations</h3>
          <p className="text-gray-600">
            Post volunteer opportunities and connect with passionate community members ready to help.
          </p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-xl font-bold mb-3 text-gray-800">Track Your Impact</h3>
          <p className="text-gray-600">
            Log your volunteer hours and see the difference you're making in the community.
          </p>
        </div>
      </div>
    </div>
  );
};

export default Home;
