import React, { createContext, useContext, useState, useEffect } from 'react';
import { loginApi, registerApi, AuthResponse } from '../api/auth';
import { getStoredToken, setStoredToken, getStoredUser, setStoredUser } from '../api/client';

export type AuthUser = {
  id: number;
  email: string;
  displayName: string;
};

type AuthContextType = {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (displayName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const storedToken = getStoredToken();
    const storedUser = getStoredUser<AuthUser>();

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(storedUser);
    }
    setIsLoading(false);
  }, []);

  const handleAuthSuccess = (res: AuthResponse) => {
    const authUser: AuthUser = {
      id: res.userId,
      email: res.email,
      displayName: res.displayName,
    };
    setToken(res.token);
    setUser(authUser);
    setStoredToken(res.token);
    setStoredUser(authUser);
  };

  const login = async (email: string, password: string) => {
    const res = await loginApi({ email, password });
    handleAuthSuccess(res);
  };

  const register = async (displayName: string, email: string, password: string) => {
    const res = await registerApi({ displayName, email, password });
    handleAuthSuccess(res);
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    setStoredToken(null);
    setStoredUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
