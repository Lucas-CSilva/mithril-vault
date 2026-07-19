"use client";

import { useState } from "react";

import { Plus } from "lucide-react";

import { Button } from "@/shared/components/ui/button";

import { CategoryForm } from "./CategoryForm";
import { CategoryList } from "./CategoryList";
import { DeleteCategoryDialog } from "./DeleteCategoryDialog";
import { useCategories } from "../hooks/useCategories";

import type { Category } from "../types";

export function CategoryManager() {
  const { data: categories } = useCategories();
  const [formOpen, setFormOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | undefined>(
    undefined,
  );
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(
    null,
  );

  function handleCreate() {
    setEditingCategory(undefined);
    setFormOpen(true);
  }

  function handleEdit(category: Category) {
    setEditingCategory(category);
    setFormOpen(true);
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-foreground text-2xl font-semibold">Categorias</h1>
          <p className="text-muted-foreground text-sm">
            Organize suas transações por categoria e subcategoria.
          </p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="h-4 w-4" />
          Nova categoria
        </Button>
      </div>

      <CategoryList onEdit={handleEdit} onDelete={setDeletingCategory} />

      <CategoryForm
        open={formOpen}
        onOpenChange={setFormOpen}
        category={editingCategory}
        categories={categories ?? []}
      />

      <DeleteCategoryDialog
        category={deletingCategory}
        onOpenChange={(open) => {
          if (!open) setDeletingCategory(null);
        }}
      />
    </div>
  );
}
