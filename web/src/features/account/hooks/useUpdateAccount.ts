import { useMutation, useQueryClient } from "@tanstack/react-query";

import { updateAccount } from "../api";
import { accountKeys } from "../keys";

import type { UpdateAccountCommand } from "../types";

export function useUpdateAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      command,
    }: {
      id: string;
      command: UpdateAccountCommand;
    }) => updateAccount(id, command),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
  });
}
