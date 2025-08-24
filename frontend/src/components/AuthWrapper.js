import React, { useState, useEffect } from 'react';
import Login from './Login';
import Register from './Register';

function AuthWrapper({ children }) {
  const [user, setUser] = useState(null);
  const [showRegister, setShowRegister] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    
    if (token && userData) {
      setUser(JSON.parse(userData));
    }
    setLoading(false);
  }, []);

  const handleLogin = (userData) => {
    setUser(userData);
  };

  const handleRegister = (userData) => {
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <div>
        {showRegister ? (
          <div>
            <Register onRegister={handleRegister} />
            <div className="text-center mt-4">
              <button
                onClick={() => setShowRegister(false)}
                className="text-indigo-600 hover:text-indigo-500"
              >
                Already have an account? Sign in
              </button>
            </div>
          </div>
        ) : (
          <div>
            <Login onLogin={handleLogin} />
            <div className="text-center mt-4">
              <button
                onClick={() => setShowRegister(true)}
                className="text-indigo-600 hover:text-indigo-500"
              >
                Don't have an account? Sign up
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  return React.cloneElement(children, { user, onLogout: handleLogout });
}

export default AuthWrapper;