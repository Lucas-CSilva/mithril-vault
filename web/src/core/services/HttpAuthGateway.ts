import { API_BASE_URL } from "@/config/api";
import type {
  AuthGateway,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from "@/core/ports/AuthGateway";

class HttpAuthGateway implements AuthGateway {
  private async request<T>(path: string, body?: unknown): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

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

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }

  login(req: LoginRequest): Promise<AuthResponse> {
    return this.request<AuthResponse>("/login", req);
  }

  register(req: RegisterRequest): Promise<AuthResponse> {
    return this.request<AuthResponse>("/register", req);
  }

  refresh(): Promise<AuthResponse> {
    return this.request<AuthResponse>("/refresh");
  }

  async logout(): Promise<void> {
    await this.request<void>("/logout");
  }
}

export const httpAuthGateway = new HttpAuthGateway();
