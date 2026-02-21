import React, { createContext, useState, useContext, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

// Role hierarchy: higher index = higher privilege
const ROLE_HIERARCHY = ['VOLUNTEER', 'ORG_ADMIN', 'ADMIN', 'SUPER_ADMIN'];

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = authService.getStoredUser();
    if (storedUser) {
      setUser(storedUser);
      // Refresh from server to pick up any role changes made since last login
      authService.getCurrentUser()
        .then(freshUser => {
          const merged = { ...storedUser, ...freshUser };
          localStorage.setItem('user', JSON.stringify(merged));
          setUser(merged);
        })
        .catch(() => {
          // Token is expired or invalid — clear stale session
          authService.logout();
          setUser(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (email, password) => {
    const userData = await authService.login(email, password);
    setUser(userData);
    return userData;
  };

  const register = async (userData) => {
    const newUser = await authService.register(userData);
    setUser(newUser);
    return newUser;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const setUserData = (userData) => {
    setUser(userData);
  };

  const updateProfile = async (data) => {
    const updated = await authService.updateProfile(data);
    const stored = authService.getStoredUser();
    const merged = { ...stored, ...updated };
    localStorage.setItem('user', JSON.stringify(merged));
    setUser(merged);
    return merged;
  };

  // Returns true if current user has at least the given role level
  const hasRole = (requiredRole) => {
    if (!user) return false;
    const userIdx = ROLE_HIERARCHY.indexOf(user.role);
    const reqIdx = ROLE_HIERARCHY.indexOf(requiredRole);
    return userIdx >= reqIdx;
  };

  const value = {
    user,
    loading,
    login,
    register,
    logout,
    setUserData,
    updateProfile,
    hasRole,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
