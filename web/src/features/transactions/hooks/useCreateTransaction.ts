import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createTransaction } from "../api";
import { transactionKeys } from "../keys";

export function useCreateTransaction() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createTransaction,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: transactionKeys.all });
      // Literal key, not an import of features/account/keys's accountKeys —
      // one feature importing another is forbidden by eslint-plugin-boundaries.
      // A transaction write changes account balances, so those reads must
      // still be invalidated (web/CLAUDE.md's cross-feature invalidation rule).
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
    },
  });
}
