import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService } from '../services/authService';
import { useAuth } from '../context/AuthContext';

const OAuthSuccess = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const { setUserData } = useAuth();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const token = searchParams.get('token');

      if (token) {
        try {
          // Store the token
          localStorage.setItem('token', token);

          // Fetch the current user data
          const userData = await authService.getCurrentUser();

          // Store user data in localStorage
          localStorage.setItem('user', JSON.stringify(userData));

          // Update the AuthContext state
          setUserData(userData);

          // Redirect to dashboard
          navigate('/dashboard');
        } catch (error) {
          console.error('OAuth authentication failed:', error);
          setError('Authentication failed. Please try again.');
          setTimeout(() => navigate('/login'), 2000);
        }
      } else {
        // No token found, redirect to login
        navigate('/login');
      }
    };

    handleOAuthCallback();
  }, [searchParams, navigate, setUserData]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        {error ? (
          <div>
            <p className="text-red-600 text-lg">{error}</p>
            <p className="mt-2 text-gray-600">Redirecting to login...</p>
          </div>
        ) : (
          <div>
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Completing authentication...</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default OAuthSuccess;
