import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="bg-blue-600 text-white shadow-lg">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <Link to="/" className="text-2xl font-bold">
            Tampa Volunteers
          </Link>

          <div className="flex items-center space-x-6">
            <Link to="/opportunities" className="hover:text-blue-200 transition">
              Opportunities
            </Link>

            {user ? (
              <>
                <Link to="/dashboard" className="hover:text-blue-200 transition">
                  Dashboard
                </Link>
                <Link to="/profile" className="hover:text-blue-200 transition">
                  Profile
                </Link>
                {hasRole('ORG_ADMIN') && !hasRole('ADMIN') && (
                  <Link to="/org-dashboard" className="hover:text-blue-200 transition">
                    My Org
                  </Link>
                )}
                {hasRole('ADMIN') && (
                  <Link to="/admin" className="hover:text-blue-200 transition">
                    Admin
                  </Link>
                )}
                <button
                  onClick={handleLogout}
                  className="bg-blue-700 hover:bg-blue-800 px-4 py-2 rounded transition"
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="hover:text-blue-200 transition">
                  Login
                </Link>
                <Link
                  to="/register"
                  className="bg-blue-700 hover:bg-blue-800 px-4 py-2 rounded transition"
                >
                  Register
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
