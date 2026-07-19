import { useMutation, useQueryClient } from "@tanstack/react-query";

import { updateCategory } from "../api";
import { categoryKeys } from "../keys";

import type { UpdateCategoryCommand } from "../types";

export function useUpdateCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      command,
    }: {
      id: string;
      command: UpdateCategoryCommand;
    }) => updateCategory(id, command),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.all });
    },
  });
}
