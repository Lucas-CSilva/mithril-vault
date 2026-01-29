export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/mithril-vault";

export const API_ENDPOINTS = {
  health: "/actuator/health",
  info: "/actuator/info",
} as const;

export const DEFAULT_REQUEST_CONFIG = {
  headers: {
    "Content-Type": "application/json",
  },
  credentials: "include" as RequestCredentials,
};

export const getApiUrl = (endpoint: string): string => {
  return `${API_BASE_URL}${endpoint}`;
};

export type ApiEndpoints = typeof API_ENDPOINTS;
