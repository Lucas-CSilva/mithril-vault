import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createCategory } from "../api";
import { categoryKeys } from "../keys";

export function useCreateCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.all });
    },
  });
}
