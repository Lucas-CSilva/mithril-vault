import { useMutation, useQueryClient } from "@tanstack/react-query";

import { reconcileAccount } from "../api";
import { accountKeys } from "../keys";

import type { ReconcileAccountCommand } from "../types";

export function useReconcileAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      command,
    }: {
      id: string;
      command: ReconcileAccountCommand;
    }) => reconcileAccount(id, command),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
