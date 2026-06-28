import { describe, expect, it, vi } from "vitest";

import { httpAuthGateway } from "../HttpAuthGateway";

const BASE_URL = "http://localhost:8080/mithril-vault";

function mockFetchOk(status: number, body?: unknown) {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      ok: true,
      status,
      json: vi.fn().mockResolvedValue(body),
    }),
  );
}

function mockFetchError(status: number, responseText: string) {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      ok: false,
      status,
      text: vi.fn().mockResolvedValue(responseText),
    }),
  );
}

describe("HttpAuthGateway", () => {
  it("login sends POST to /login with JSON body and credentials", async () => {
    mockFetchOk(200, { email: "user@test.com", displayName: "Test User" });

    await httpAuthGateway.login({
      email: "user@test.com",
      password: "pass123",
    });

    expect(fetch).toHaveBeenCalledWith(`${BASE_URL}/login`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "user@test.com", password: "pass123" }),
    });
  });

  it("login returns the AuthResponse on success", async () => {
    const authResponse = { email: "user@test.com", displayName: "Test User" };
    mockFetchOk(200, authResponse);

    const result = await httpAuthGateway.login({
      email: "user@test.com",
      password: "pass123",
    });

    expect(result).toEqual(authResponse);
  });

  it("register sends POST to /register with full body", async () => {
    mockFetchOk(200, { email: "user@test.com", displayName: "Test User" });

    await httpAuthGateway.register({
      email: "user@test.com",
      password: "pass123",
      displayName: "Test User",
    });

    expect(fetch).toHaveBeenCalledWith(`${BASE_URL}/register`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: "user@test.com",
        password: "pass123",
        displayName: "Test User",
      }),
    });
  });

  it("refresh sends POST to /refresh without a body", async () => {
    mockFetchOk(200, { email: "user@test.com", displayName: "Test User" });

    await httpAuthGateway.refresh();

    expect(fetch).toHaveBeenCalledWith(`${BASE_URL}/refresh`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: undefined,
    });
  });

  it("logout resolves void on 204 response", async () => {
    mockFetchOk(204);

    await expect(httpAuthGateway.logout()).resolves.toBeUndefined();
  });

  it("throws the API error message on a non-ok JSON response", async () => {
    mockFetchError(401, JSON.stringify({ message: "Credenciais inválidas" }));

    await expect(
      httpAuthGateway.login({ email: "x@x.com", password: "wrong" }),
    ).rejects.toThrow("Credenciais inválidas");
  });

  it("throws a generic HTTP error when response body has no message", async () => {
    mockFetchError(500, "Internal Server Error");

    await expect(
      httpAuthGateway.login({ email: "x@x.com", password: "pass" }),
    ).rejects.toThrow("HTTP 500");
  });
});
