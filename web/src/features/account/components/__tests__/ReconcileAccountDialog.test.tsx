import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { centavos } from "@/shared/utils";

import { ReconcileAccountDialog } from "../ReconcileAccountDialog";

import type { Account } from "../../types";

const mockMutateAsync = vi.hoisted(() => vi.fn());

vi.mock("../../hooks/useReconcileAccount", () => ({
  useReconcileAccount: () => ({ mutateAsync: mockMutateAsync }),
}));

const account = {
  id: "a1",
  name: "Nubank",
  type: "DIGITAL",
  institution: "Nubank",
  initialBalance: centavos(100000),
  currentBalance: centavos(148500),
  color: "#3C5070",
  isActive: true,
  createdAt: "2026-01-01T00:00:00Z",
} satisfies Account;

describe("ReconcileAccountDialog", () => {
  it("does not render when there is no account selected", () => {
    render(<ReconcileAccountDialog account={null} onOpenChange={vi.fn()} />);

    expect(screen.queryByText("Reconciliar conta")).not.toBeInTheDocument();
  });

  it("submits the real balance converted to centavos, always using ADJUST_INITIAL_BALANCE", async () => {
    const user = userEvent.setup();
    mockMutateAsync.mockResolvedValue(undefined);
    const onOpenChange = vi.fn();

    render(
      <ReconcileAccountDialog account={account} onOpenChange={onOpenChange} />,
    );

    await user.type(screen.getByLabelText("Saldo real (R$)"), "1485,00");
    await user.click(screen.getByRole("button", { name: "Reconciliar" }));

    await waitFor(() =>
      expect(mockMutateAsync).toHaveBeenCalledWith({
        id: "a1",
        command: { realBalance: 148500, method: "ADJUST_INITIAL_BALANCE" },
      }),
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("displays a server error when reconciliation fails", async () => {
    const user = userEvent.setup();
    mockMutateAsync.mockRejectedValue(new Error("Conta não encontrada"));

    render(<ReconcileAccountDialog account={account} onOpenChange={vi.fn()} />);

    await user.type(screen.getByLabelText("Saldo real (R$)"), "100,00");
    await user.click(screen.getByRole("button", { name: "Reconciliar" }));

    await waitFor(() =>
      expect(screen.getByText("Conta não encontrada")).toBeInTheDocument(),
    );
  });
});
