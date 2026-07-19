import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { CategoryForm } from "../CategoryForm";

const mockCreateMutateAsync = vi.hoisted(() => vi.fn());
const mockUpdateMutateAsync = vi.hoisted(() => vi.fn());

vi.mock("../../hooks/useCreateCategory", () => ({
  useCreateCategory: () => ({ mutateAsync: mockCreateMutateAsync }),
}));

vi.mock("../../hooks/useUpdateCategory", () => ({
  useUpdateCategory: () => ({ mutateAsync: mockUpdateMutateAsync }),
}));

describe("CategoryForm", () => {
  it("shows a validation error when name is empty", async () => {
    const user = userEvent.setup();
    render(<CategoryForm open onOpenChange={vi.fn()} categories={[]} />);

    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(screen.getByText("Nome obrigatório")).toBeInTheDocument(),
    );
    expect(mockCreateMutateAsync).not.toHaveBeenCalled();
  });

  it("creates a category with the entered name", async () => {
    const user = userEvent.setup();
    mockCreateMutateAsync.mockResolvedValue(undefined);
    const onOpenChange = vi.fn();

    render(<CategoryForm open onOpenChange={onOpenChange} categories={[]} />);

    await user.type(screen.getByLabelText("Nome"), "Pets");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(mockCreateMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({ name: "Pets" }),
      ),
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("displays a server error when creation fails", async () => {
    const user = userEvent.setup();
    mockCreateMutateAsync.mockRejectedValue(new Error("Categoria já existe"));

    render(<CategoryForm open onOpenChange={vi.fn()} categories={[]} />);

    await user.type(screen.getByLabelText("Nome"), "Pets");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(screen.getByText("Categoria já existe")).toBeInTheDocument(),
    );
  });

  it("hides the parent selector and calls update when editing", async () => {
    const user = userEvent.setup();
    mockUpdateMutateAsync.mockResolvedValue(undefined);

    render(
      <CategoryForm
        open
        onOpenChange={vi.fn()}
        categories={[]}
        category={{
          id: "c2",
          name: "Pets",
          parentId: null,
          icon: null,
          color: "#4E7C66",
          isSystem: false,
        }}
      />,
    );

    expect(screen.getByDisplayValue("Pets")).toBeInTheDocument();
    expect(screen.queryByText("Categoria principal")).not.toBeInTheDocument();

    await user.clear(screen.getByLabelText("Nome"));
    await user.type(screen.getByLabelText("Nome"), "Animais");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(mockUpdateMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          id: "c2",
          command: expect.objectContaining({ name: "Animais" }),
        }),
      ),
    );
  });
});
