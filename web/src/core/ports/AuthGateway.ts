export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface AuthResponse {
  email: string;
  displayName: string;
}

export interface AuthGateway {
  login(request: LoginRequest): Promise<AuthResponse>;
  register(request: RegisterRequest): Promise<AuthResponse>;
  refresh(): Promise<AuthResponse>;
  logout(): Promise<void>;
}
