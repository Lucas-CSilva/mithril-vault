import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { AccountList } from "../AccountList";

const mockUseAccounts = vi.hoisted(() => vi.fn());
const mockUseAccountBalanceHistory = vi.hoisted(() => vi.fn());

vi.mock("../../hooks/useAccounts", () => ({
  useAccounts: mockUseAccounts,
}));

vi.mock("../../hooks/useAccountBalanceHistory", () => ({
  useAccountBalanceHistory: mockUseAccountBalanceHistory,
}));

const activeAccount = {
  id: "a1",
  name: "Nubank",
  type: "DIGITAL",
  institution: "Nubank",
  initialBalance: 100000,
  currentBalance: 148500,
  color: "#3C5070",
  isActive: true,
  createdAt: "2026-01-01T00:00:00Z",
};

const inactiveAccount = {
  id: "a2",
  name: "Conta Antiga",
  type: "CHECKING",
  institution: null,
  initialBalance: 0,
  currentBalance: 0,
  color: null,
  isActive: false,
  createdAt: "2025-01-01T00:00:00Z",
};

describe("AccountList", () => {
  beforeEach(() => {
    mockUseAccountBalanceHistory.mockReturnValue({ data: undefined });
  });

  it("shows a loading state while pending", () => {
    mockUseAccounts.mockReturnValue({
      data: undefined,
      isPending: true,
      isError: false,
      refetch: vi.fn(),
    });

    render(
      <AccountList
        includeInactive={false}
        onEdit={vi.fn()}
        onReconcile={vi.fn()}
        onDeactivate={vi.fn()}
        onReactivate={vi.fn()}
      />,
    );

    expect(screen.queryByText("Nubank")).not.toBeInTheDocument();
  });

  it("shows an error state with a retry action", async () => {
    const user = userEvent.setup();
    const refetch = vi.fn();
    mockUseAccounts.mockReturnValue({
      data: undefined,
      isPending: false,
      isError: true,
      refetch,
    });

    render(
      <AccountList
        includeInactive={false}
        onEdit={vi.fn()}
        onReconcile={vi.fn()}
        onDeactivate={vi.fn()}
        onReactivate={vi.fn()}
      />,
    );

    expect(
      screen.getByText("Não foi possível carregar as contas."),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(refetch).toHaveBeenCalled();
  });

  it("shows an empty state when there are no accounts", () => {
    mockUseAccounts.mockReturnValue({
      data: [],
      isPending: false,
      isError: false,
      refetch: vi.fn(),
    });

    render(
      <AccountList
        includeInactive={false}
        onEdit={vi.fn()}
        onReconcile={vi.fn()}
        onDeactivate={vi.fn()}
        onReactivate={vi.fn()}
      />,
    );

    expect(screen.getByText("Nenhuma conta encontrada.")).toBeInTheDocument();
  });

  it("renders accounts with balance and shows a reactivate action for inactive ones", () => {
    mockUseAccounts.mockReturnValue({
      data: [activeAccount, inactiveAccount],
      isPending: false,
      isError: false,
      refetch: vi.fn(),
    });

    render(
      <AccountList
        includeInactive
        onEdit={vi.fn()}
        onReconcile={vi.fn()}
        onDeactivate={vi.fn()}
        onReactivate={vi.fn()}
      />,
    );

    expect(screen.getByText("Nubank")).toBeInTheDocument();
    expect(screen.getByText("Conta Antiga")).toBeInTheDocument();
    expect(screen.getByText("Inativa")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Reativar Conta Antiga" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Desativar Conta Antiga" }),
    ).not.toBeInTheDocument();
  });
});
