import { httpApiClient } from "@/core/services/HttpApiClient";

import type {
  Category,
  CreateCategoryCommand,
  UpdateCategoryCommand,
} from "./types";

export function listCategories(): Promise<Category[]> {
  return httpApiClient.get<Category[]>("/categories");
}

export function createCategory(
  command: CreateCategoryCommand,
): Promise<Category> {
  return httpApiClient.post<Category>("/categories", command);
}

export function updateCategory(
  id: string,
  command: UpdateCategoryCommand,
): Promise<Category> {
  return httpApiClient.patch<Category>(`/categories/${id}`, command);
}

export function deleteCategory(id: string): Promise<void> {
  return httpApiClient.delete<void>(`/categories/${id}`);
}
