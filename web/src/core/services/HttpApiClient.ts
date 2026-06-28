import { API_BASE_URL } from "@/config/api";
import type { ApiClient } from "@/core/ports/ApiClient";

class HttpApiClient implements ApiClient {
  private async request<T>(
    path: string,
    init: RequestInit,
    isRetry = false,
  ): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        ...init.headers,
      },
    });

    if (response.status === 401 && !isRetry) {
      const refreshed = await fetch(`${API_BASE_URL}/refresh`, {
        method: "POST",
        credentials: "include",
      });

      if (!refreshed.ok) {
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
        throw new Error("Session expired");
      }

      return this.request<T>(path, init, true);
    }

    if (!response.ok) {
      const text = await response.text();
      let message = `HTTP ${response.status}`;
      try {
        const json = JSON.parse(text) as {
          errors?: { message: string }[];
          message?: string;
        };
        if (json.errors?.[0]?.message) message = json.errors[0].message;
        else if (json.message) message = json.message;
      } catch {
        // plain text error body
      }
      throw new Error(message);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json() as Promise<T>;
  }

  get<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "GET" });
  }

  post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "POST",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "PUT",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  patch<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "PATCH",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  delete<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "DELETE" });
  }
}

export const httpApiClient = new HttpApiClient();
