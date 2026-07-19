import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { CategoryList } from "../CategoryList";

const mockUseCategories = vi.hoisted(() => vi.fn());

vi.mock("../../hooks/useCategories", () => ({
  useCategories: mockUseCategories,
}));

const systemCategory = {
  id: "c1",
  name: "Alimentação",
  parentId: null,
  icon: "cart",
  color: "#B0795F",
  isSystem: true,
};

const userCategory = {
  id: "c2",
  name: "Pets",
  parentId: null,
  icon: null,
  color: "#4E7C66",
  isSystem: false,
};

describe("CategoryList", () => {
  it("shows a loading state while pending", () => {
    mockUseCategories.mockReturnValue({
      data: undefined,
      isPending: true,
      isError: false,
      refetch: vi.fn(),
    });

    render(<CategoryList onEdit={vi.fn()} onDelete={vi.fn()} />);

    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("shows an error state with a retry action", async () => {
    const user = userEvent.setup();
    const refetch = vi.fn();
    mockUseCategories.mockReturnValue({
      data: undefined,
      isPending: false,
      isError: true,
      refetch,
    });

    render(<CategoryList onEdit={vi.fn()} onDelete={vi.fn()} />);

    expect(
      screen.getByText("Não foi possível carregar as categorias."),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(refetch).toHaveBeenCalled();
  });

  it("shows an empty state when there are no categories", () => {
    mockUseCategories.mockReturnValue({
      data: [],
      isPending: false,
      isError: false,
      refetch: vi.fn(),
    });

    render(<CategoryList onEdit={vi.fn()} onDelete={vi.fn()} />);

    expect(
      screen.getByText("Nenhuma categoria encontrada."),
    ).toBeInTheDocument();
  });

  it("renders categories and disables actions for system categories", () => {
    mockUseCategories.mockReturnValue({
      data: [systemCategory, userCategory],
      isPending: false,
      isError: false,
      refetch: vi.fn(),
    });

    render(<CategoryList onEdit={vi.fn()} onDelete={vi.fn()} />);

    expect(screen.getByText("Alimentação")).toBeInTheDocument();
    expect(screen.getByText("Pets")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Editar Alimentação" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Editar Pets" }),
    ).not.toBeDisabled();
  });
});
