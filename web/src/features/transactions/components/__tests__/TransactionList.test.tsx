import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ApiError } from "@/core/services/HttpApiClient";

import { TransactionList } from "../TransactionList";

const mockUseTransactions = vi.hoisted(() => vi.fn());

vi.mock("../../hooks/useTransactions", () => ({
  useTransactions: mockUseTransactions,
}));

describe("TransactionList", () => {
  it("shows a loading state while pending", () => {
    mockUseTransactions.mockReturnValue({
      data: undefined,
      isPending: true,
      isError: false,
      error: null,
    });

    render(<TransactionList />);

    expect(screen.queryByText("Mercado")).not.toBeInTheDocument();
  });

  it("shows a gated message when the backend hasn't shipped GET /transactions yet (405)", () => {
    mockUseTransactions.mockReturnValue({
      data: undefined,
      isPending: false,
      isError: true,
      error: new ApiError("HTTP 405", 405),
    });

    render(<TransactionList />);

    expect(
      screen.getByText("Listagem de transações ainda não disponível"),
    ).toBeInTheDocument();
  });

  it("also treats a 404 as the gated not-yet-available state", () => {
    mockUseTransactions.mockReturnValue({
      data: undefined,
      isPending: false,
      isError: true,
      error: new ApiError("HTTP 404", 404),
    });

    render(<TransactionList />);

    expect(
      screen.getByText("Listagem de transações ainda não disponível"),
    ).toBeInTheDocument();
  });

  it("shows a generic error state for a non-404 failure", () => {
    mockUseTransactions.mockReturnValue({
      data: undefined,
      isPending: false,
      isError: true,
      error: new Error("boom"),
    });

    render(<TransactionList />);

    expect(
      screen.getByText("Não foi possível carregar as transações."),
    ).toBeInTheDocument();
  });

  it("shows an empty state when there are no transactions", () => {
    mockUseTransactions.mockReturnValue({
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
      isPending: false,
      isError: false,
      error: null,
    });

    render(<TransactionList />);

    expect(
      screen.getByText("Nenhuma transação encontrada."),
    ).toBeInTheDocument();
  });

  it("renders transactions with signed amounts", () => {
    mockUseTransactions.mockReturnValue({
      data: {
        content: [
          {
            id: "t1",
            type: "DEBIT",
            amount: 5000,
            date: "2026-06-01",
            description: "Mercado",
            categoryId: null,
            paymentMethod: "PIX",
            accountId: "acc1",
            invoiceId: null,
            account: { id: "acc1", name: "Nubank" },
            card: null,
            invoice: null,
            tags: [],
            notes: null,
            createdAt: "2026-06-01T00:00:00Z",
          },
        ],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      },
      isPending: false,
      isError: false,
      error: null,
    });

    render(<TransactionList />);

    expect(screen.getByText("Mercado")).toBeInTheDocument();
    expect(screen.getByText("Nubank")).toBeInTheDocument();
    expect(screen.getByText(/-R\$\s?50,00/)).toBeInTheDocument();
  });
});
