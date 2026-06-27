import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { LoginForm } from "../LoginForm";

const mockLogin = vi.hoisted(() => vi.fn());

vi.mock("@/core/contexts/AuthContext", () => ({
  useAuth: () => ({ login: mockLogin }),
}));

vi.mock("next/link", () => ({
  default: ({
    href,
    children,
    ...props
  }: React.AnchorHTMLAttributes<HTMLAnchorElement> & { href: string }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

describe("LoginForm", () => {
  it("renders email and password fields", () => {
    render(<LoginForm />);
    expect(screen.getByPlaceholderText("voce@email.com")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("••••••••")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Entrar" })).toBeInTheDocument();
  });

  it("shows email validation error when email is invalid", async () => {
    const user = userEvent.setup();
    render(<LoginForm />);

    await user.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() =>
      expect(screen.getByText("Email inválido")).toBeInTheDocument(),
    );
  });

  it("shows password validation error when password is too short", async () => {
    const user = userEvent.setup();
    render(<LoginForm />);

    await user.type(
      screen.getByPlaceholderText("voce@email.com"),
      "user@test.com",
    );
    await user.type(screen.getByPlaceholderText("••••••••"), "short");
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() =>
      expect(
        screen.getByText("Senha deve ter ao menos 8 caracteres"),
      ).toBeInTheDocument(),
    );
  });

  it("calls login with email and password on valid submit", async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue(undefined);
    render(<LoginForm />);

    await user.type(
      screen.getByPlaceholderText("voce@email.com"),
      "user@test.com",
    );
    await user.type(screen.getByPlaceholderText("••••••••"), "validpassword");
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() =>
      expect(mockLogin).toHaveBeenCalledWith({
        email: "user@test.com",
        password: "validpassword",
      }),
    );
  });

  it("displays server error message when login fails", async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue(new Error("Credenciais inválidas"));
    render(<LoginForm />);

    await user.type(
      screen.getByPlaceholderText("voce@email.com"),
      "user@test.com",
    );
    await user.type(screen.getByPlaceholderText("••••••••"), "validpassword");
    await user.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() =>
      expect(screen.getByText("Credenciais inválidas")).toBeInTheDocument(),
    );
  });
});
