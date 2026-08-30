import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { TransactionForm } from "../TransactionForm";

const mockMutateAsync = vi.hoisted(() => vi.fn());

vi.mock("../../hooks/useCreateTransaction", () => ({
  useCreateTransaction: () => ({ mutateAsync: mockMutateAsync }),
}));

vi.mock("../../hooks/useAccountOptions", () => ({
  useAccountOptions: () => ({
    data: [
      { id: "acc-1", name: "Nubank" },
      { id: "acc-2", name: "Itaú" },
    ],
    refetch: vi.fn(),
  }),
}));

vi.mock("../../hooks/useCategoryOptions", () => ({
  useCategoryOptions: () => ({
    data: [{ id: "cat-1", name: "Mercado" }],
    refetch: vi.fn(),
  }),
}));

async function selectOption(
  user: ReturnType<typeof userEvent.setup>,
  triggerName: string,
  optionName: string,
) {
  await user.click(screen.getByRole("combobox", { name: triggerName }));
  await user.click(await screen.findByRole("option", { name: optionName }));
}

describe("TransactionForm", () => {
  it("shows validation errors when required fields are missing", async () => {
    const user = userEvent.setup();
    render(<TransactionForm open onOpenChange={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(screen.getByText("Descrição obrigatória")).toBeInTheDocument(),
    );
    expect(screen.getByText("Conta obrigatória")).toBeInTheDocument();
    expect(
      screen.getByText("Forma de pagamento obrigatória"),
    ).toBeInTheDocument();
    expect(mockMutateAsync).not.toHaveBeenCalled();
  });

  it("creates a SINGLE transaction converting reais to centavos", async () => {
    const user = userEvent.setup();
    mockMutateAsync.mockResolvedValue([{}]);
    const onOpenChange = vi.fn();

    render(<TransactionForm open onOpenChange={onOpenChange} />);

    await user.type(screen.getByLabelText("Descrição"), "Supermercado");
    await user.type(screen.getByLabelText("Valor (R$)"), "150,90");
    await selectOption(user, "Conta", "Nubank");
    await selectOption(user, "Forma de pagamento", "Pix");

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          mode: "SINGLE",
          type: "DEBIT",
          amount: 15090,
          description: "Supermercado",
          accountId: "acc-1",
          paymentMethod: "PIX",
        }),
      ),
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("hides type and payment method for TRANSFER and fixes them server-side", async () => {
    const user = userEvent.setup();
    mockMutateAsync.mockResolvedValue([{}, {}]);
    const onOpenChange = vi.fn();

    render(<TransactionForm open onOpenChange={onOpenChange} />);

    await user.click(screen.getByRole("radio", { name: "Transferência" }));

    expect(
      screen.queryByRole("radio", { name: "Saída" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Forma de pagamento"),
    ).not.toBeInTheDocument();

    await user.type(screen.getByLabelText("Descrição"), "Transferência mensal");
    await user.type(screen.getByLabelText("Valor (R$)"), "100,00");
    await selectOption(user, "Conta de origem", "Nubank");
    await selectOption(user, "Conta de destino", "Itaú");

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          mode: "TRANSFER",
          type: "DEBIT",
          paymentMethod: "TRANSFER",
          accountId: "acc-1",
          transfer: { destinationAccountId: "acc-2" },
        }),
      ),
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("rejects a recurring end date before the transaction date", async () => {
    const user = userEvent.setup();
    render(<TransactionForm open onOpenChange={vi.fn()} />);

    await user.click(screen.getByRole("radio", { name: "Recorrente" }));

    fireEvent.change(screen.getByLabelText("Data"), {
      target: { value: "2026-06-10" },
    });
    fireEvent.change(screen.getByLabelText("Data final (opcional)"), {
      target: { value: "2026-06-01" },
    });
    await selectOption(user, "Frequência", "Mensal");

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(
        screen.getByText("Data final não pode ser antes da data da transação"),
      ).toBeInTheDocument(),
    );
    expect(mockMutateAsync).not.toHaveBeenCalled();
  });

  it("displays a server error when creation fails", async () => {
    const user = userEvent.setup();
    mockMutateAsync.mockRejectedValue(
      new Error("endDate must not be before date"),
    );

    render(<TransactionForm open onOpenChange={vi.fn()} />);

    await user.type(screen.getByLabelText("Descrição"), "Supermercado");
    await user.type(screen.getByLabelText("Valor (R$)"), "50,00");
    await selectOption(user, "Conta", "Nubank");
    await selectOption(user, "Forma de pagamento", "Pix");

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(
        screen.getByText("endDate must not be before date"),
      ).toBeInTheDocument(),
    );
  });
});
