import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createAccount } from "../api";
import { accountKeys } from "../keys";

export function useCreateAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
