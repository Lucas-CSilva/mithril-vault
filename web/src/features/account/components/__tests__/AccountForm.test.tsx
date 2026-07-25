import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { centavos } from "@/shared/utils";

import { AccountForm } from "../AccountForm";

import type { Account } from "../../types";

const mockCreateMutateAsync = vi.hoisted(() => vi.fn());
const mockUpdateMutateAsync = vi.hoisted(() => vi.fn());

vi.mock("../../hooks/useCreateAccount", () => ({
  useCreateAccount: () => ({ mutateAsync: mockCreateMutateAsync }),
}));

vi.mock("../../hooks/useUpdateAccount", () => ({
  useUpdateAccount: () => ({ mutateAsync: mockUpdateMutateAsync }),
}));

describe("AccountForm", () => {
  it("shows a validation error when name is empty", async () => {
    const user = userEvent.setup();
    render(<AccountForm open onOpenChange={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(screen.getByText("Nome obrigatório")).toBeInTheDocument(),
    );
    expect(mockCreateMutateAsync).not.toHaveBeenCalled();
  });

  it("creates an account converting the reais input to centavos", async () => {
    const user = userEvent.setup();
    mockCreateMutateAsync.mockResolvedValue(undefined);
    const onOpenChange = vi.fn();

    render(<AccountForm open onOpenChange={onOpenChange} />);

    await user.type(screen.getByLabelText("Nome"), "Nubank");
    await user.type(screen.getByLabelText("Saldo inicial (R$)"), "1500,90");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(mockCreateMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({ name: "Nubank", initialBalance: 150090 }),
      ),
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("displays a server error when creation fails", async () => {
    const user = userEvent.setup();
    mockCreateMutateAsync.mockRejectedValue(new Error("Conta já existe"));

    render(<AccountForm open onOpenChange={vi.fn()} />);

    await user.type(screen.getByLabelText("Nome"), "Nubank");
    await user.type(screen.getByLabelText("Saldo inicial (R$)"), "100,00");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(screen.getByText("Conta já existe")).toBeInTheDocument(),
    );
  });

  it("hides the initial balance field and calls update when editing", async () => {
    const user = userEvent.setup();
    mockUpdateMutateAsync.mockResolvedValue(undefined);

    render(
      <AccountForm
        open
        onOpenChange={vi.fn()}
        account={
          {
            id: "a1",
            name: "Nubank",
            type: "DIGITAL",
            institution: "Banco Nubank S.A.",
            initialBalance: centavos(100000),
            currentBalance: centavos(148500),
            color: "#3C5070",
            isActive: true,
            createdAt: "2026-01-01T00:00:00Z",
          } satisfies Account
        }
      />,
    );

    expect(screen.getByDisplayValue("Nubank")).toBeInTheDocument();
    expect(
      screen.queryByLabelText("Saldo inicial (R$)"),
    ).not.toBeInTheDocument();

    await user.clear(screen.getByLabelText("Nome"));
    await user.type(screen.getByLabelText("Nome"), "Nubank Conta");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(mockUpdateMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          id: "a1",
          command: expect.objectContaining({ name: "Nubank Conta" }),
        }),
      ),
    );
  });
});
