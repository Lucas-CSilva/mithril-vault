"use client";

import { Pencil, Trash2 } from "lucide-react";

import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/components/ui/table";
import { cn } from "@/shared/utils";

import { useCategories } from "../hooks/useCategories";
import { DEFAULT_CATEGORY_COLOR } from "../lib/colors";
import { CategoryIcon } from "../lib/icon-map";

import type { Category } from "../types";

interface CategoryListProps {
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
}

export function CategoryList({ onEdit, onDelete }: CategoryListProps) {
  const { data: categories, isPending, isError, refetch } = useCategories();

  if (isPending) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 5 }).map((_, index) => (
          <div
            key={index}
            className="bg-muted h-10 w-full animate-pulse rounded-md"
          />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="border-destructive/30 bg-destructive/5 flex flex-col items-center gap-3 rounded-md border p-8 text-center">
        <p className="text-muted-foreground text-sm">
          Não foi possível carregar as categorias.
        </p>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          Tentar novamente
        </Button>
      </div>
    );
  }

  if (categories.length === 0) {
    return (
      <div className="text-muted-foreground rounded-md border border-dashed p-8 text-center text-sm">
        Nenhuma categoria encontrada.
      </div>
    );
  }

  const topLevel = categories.filter((category) => !category.parentId);
  const childrenByParent = new Map<string, Category[]>();
  for (const category of categories) {
    if (!category.parentId) continue;
    const siblings = childrenByParent.get(category.parentId) ?? [];
    siblings.push(category);
    childrenByParent.set(category.parentId, siblings);
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Categoria</TableHead>
          <TableHead>Tipo</TableHead>
          <TableHead className="text-right">Ações</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {topLevel.map((category) => (
          <CategoryRows
            key={category.id}
            category={category}
            subcategories={childrenByParent.get(category.id) ?? []}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        ))}
      </TableBody>
    </Table>
  );
}

interface CategoryRowsProps {
  category: Category;
  subcategories: Category[];
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
}

function CategoryRows({
  category,
  subcategories,
  onEdit,
  onDelete,
}: CategoryRowsProps) {
  return (
    <>
      <CategoryRow category={category} onEdit={onEdit} onDelete={onDelete} />
      {subcategories.map((child) => (
        <CategoryRow
          key={child.id}
          category={child}
          onEdit={onEdit}
          onDelete={onDelete}
          isSubcategory
        />
      ))}
    </>
  );
}

interface CategoryRowProps {
  category: Category;
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
  isSubcategory?: boolean;
}

function CategoryRow({
  category,
  onEdit,
  onDelete,
  isSubcategory,
}: CategoryRowProps) {
  return (
    <TableRow>
      <TableCell>
        <div className={cn("flex items-center gap-2", isSubcategory && "pl-6")}>
          <span
            className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-white"
            style={{
              backgroundColor: category.color ?? DEFAULT_CATEGORY_COLOR,
            }}
          >
            <CategoryIcon icon={category.icon} className="h-3.5 w-3.5" />
          </span>
          <span>{category.name}</span>
        </div>
      </TableCell>
      <TableCell>
        {category.isSystem ? (
          <Badge variant="secondary">Sistema</Badge>
        ) : (
          <Badge variant="outline">Personalizada</Badge>
        )}
      </TableCell>
      <TableCell className="text-right">
        <div className="flex justify-end gap-1">
          <Button
            variant="ghost"
            size="icon"
            disabled={category.isSystem}
            onClick={() => onEdit(category)}
            aria-label={`Editar ${category.name}`}
          >
            <Pencil className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            disabled={category.isSystem}
            onClick={() => onDelete(category)}
            aria-label={`Excluir ${category.name}`}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}
