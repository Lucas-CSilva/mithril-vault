import { useMutation, useQueryClient } from "@tanstack/react-query";

import { deleteCategory } from "../api";
import { categoryKeys } from "../keys";

export function useDeleteCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.all });
    },
  });
}
