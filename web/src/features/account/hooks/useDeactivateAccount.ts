import { useMutation, useQueryClient } from "@tanstack/react-query";

import { deactivateAccount } from "../api";
import { accountKeys } from "../keys";

export function useDeactivateAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deactivateAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
