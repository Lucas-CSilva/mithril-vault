import { httpAuthGateway } from "@/core/services/HttpAuthGateway";

import type { LoginRequest, RegisterRequest } from "./types";

export const login = (req: LoginRequest) => httpAuthGateway.login(req);
export const register = (req: RegisterRequest) => httpAuthGateway.register(req);
export const logout = () => httpAuthGateway.logout();
