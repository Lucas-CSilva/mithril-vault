"use client";

import { useState } from "react";

import { Button } from "@/shared/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/components/ui/dialog";

import { useDeleteCategory } from "../hooks/useDeleteCategory";

import type { Category } from "../types";

interface DeleteCategoryDialogProps {
  category: Category | null;
  onOpenChange: (open: boolean) => void;
}

export function DeleteCategoryDialog({
  category,
  onOpenChange,
}: DeleteCategoryDialogProps) {
  const [serverError, setServerError] = useState<string | null>(null);
  const deleteCategory = useDeleteCategory();

  async function handleConfirm() {
    if (!category) return;
    setServerError(null);
    try {
      await deleteCategory.mutateAsync(category.id);
      onOpenChange(false);
    } catch (err) {
      setServerError(
        err instanceof Error ? err.message : "Erro ao excluir categoria",
      );
    }
  }

  return (
    <Dialog open={category !== null} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Excluir categoria</DialogTitle>
          <DialogDescription>
            Tem certeza que deseja excluir &ldquo;{category?.name}&rdquo;? Todas
            as transações associadas a ela (e a suas subcategorias) serão
            reatribuídas para &ldquo;Outros&rdquo;.
          </DialogDescription>
        </DialogHeader>

        {serverError && (
          <div className="border-destructive/30 bg-destructive/5 text-destructive rounded-md border p-3 text-sm">
            {serverError}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button
            variant="destructive"
            disabled={deleteCategory.isPending}
            onClick={handleConfirm}
          >
            {deleteCategory.isPending ? "Excluindo…" : "Excluir"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
