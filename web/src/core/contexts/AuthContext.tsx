"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";

import { useRouter } from "next/navigation";

import type { LoginRequest, RegisterRequest } from "@/core/ports/AuthGateway";
import { httpAuthGateway } from "@/core/services/HttpAuthGateway";

interface AuthUser {
  email: string;
  displayName: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login(req: LoginRequest): Promise<void>;
  register(req: RegisterRequest): Promise<void>;
  logout(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    httpAuthGateway
      .refresh()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false));
  }, []);

  const login = useCallback(
    async (req: LoginRequest) => {
      const res = await httpAuthGateway.login(req);
      setUser(res);
      router.push("/");
    },
    [router],
  );

  const register = useCallback(
    async (req: RegisterRequest) => {
      const res = await httpAuthGateway.register(req);
      setUser(res);
      router.push("/");
    },
    [router],
  );

  const logout = useCallback(async () => {
    await httpAuthGateway.logout().catch(() => {});
    setUser(null);
    router.push("/login");
  }, [router]);

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
