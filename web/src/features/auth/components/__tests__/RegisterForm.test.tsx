import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { RegisterForm } from "../RegisterForm";

const mockRegister = vi.hoisted(() => vi.fn());

vi.mock("@/core/contexts/AuthContext", () => ({
  useAuth: () => ({ register: mockRegister }),
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

describe("RegisterForm", () => {
  it("renders name, email and password fields", () => {
    render(<RegisterForm />);
    expect(
      screen.getByPlaceholderText("Como podemos te chamar?"),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText("voce@email.com")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("Mínimo de 8 caracteres"),
    ).toBeInTheDocument();
  });

  it("shows name required error when display name is empty", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() =>
      expect(screen.getByText("Nome obrigatório")).toBeInTheDocument(),
    );
  });

  it("shows email validation error when email is invalid", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.type(
      screen.getByPlaceholderText("Como podemos te chamar?"),
      "Test User",
    );
    await user.type(
      screen.getByPlaceholderText("voce@email.com"),
      "notanemail",
    );
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() =>
      expect(screen.getByText("Email inválido")).toBeInTheDocument(),
    );
  });

  it("shows password validation error when password is too short", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.type(
      screen.getByPlaceholderText("Como podemos te chamar?"),
      "Test User",
    );
    await user.type(
      screen.getByPlaceholderText("voce@email.com"),
      "user@test.com",
    );
    await user.type(
      screen.getByPlaceholderText("Mínimo de 8 caracteres"),
      "short",
    );
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() =>
      expect(
        screen.getByText("Senha deve ter ao menos 8 caracteres"),
      ).toBeInTheDocument(),
    );
  });

  it("updates the password strength meter as the user types", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    expect(screen.getByText("Força da senha")).toBeInTheDocument();

    await user.type(
      screen.getByPlaceholderText("Mínimo de 8 caracteres"),
      "ab",
    );
    await waitFor(() =>
      expect(screen.getByText("Muito fraca")).toBeInTheDocument(),
    );

    await user.type(
      screen.getByPlaceholderText("Mínimo de 8 caracteres"),
      "CD12!@zzzz",
    );
    await waitFor(() => expect(screen.getByText("Forte")).toBeInTheDocument());
  });

  it("calls register with correct values on valid submit", async () => {
    const user = userEvent.setup();
    mockRegister.mockResolvedValue(undefined);
    render(<RegisterForm />);

    await user.type(
      screen.getByPlaceholderText("Como podemos te chamar?"),
      "Test User",
    );
    await user.type(
      screen.getByPlaceholderText("voce@email.com"),
      "user@test.com",
    );
    await user.type(
      screen.getByPlaceholderText("Mínimo de 8 caracteres"),
      "validpassword",
    );
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() =>
      expect(mockRegister).toHaveBeenCalledWith({
        displayName: "Test User",
        email: "user@test.com",
        password: "validpassword",
      }),
    );
    expect(mockRegister.mock.calls[0][0]).not.toHaveProperty("terms");
  });

  it("displays server error message when registration fails", async () => {
    const user = userEvent.setup();
    mockRegister.mockRejectedValue(new Error("E-mail já cadastrado"));
    render(<RegisterForm />);

    await user.type(
      screen.getByPlaceholderText("Como podemos te chamar?"),
      "Test User",
    );
    await user.type(
      screen.getByPlaceholderText("voce@email.com"),
      "user@test.com",
    );
    await user.type(
      screen.getByPlaceholderText("Mínimo de 8 caracteres"),
      "validpassword",
    );
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() =>
      expect(screen.getByText("E-mail já cadastrado")).toBeInTheDocument(),
    );
  });
});
