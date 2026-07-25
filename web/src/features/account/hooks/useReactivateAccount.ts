import { useMutation, useQueryClient } from "@tanstack/react-query";

import { reactivateAccount } from "../api";
import { accountKeys } from "../keys";

export function useReactivateAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: reactivateAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
