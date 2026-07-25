import { useQuery } from "@tanstack/react-query";

import { listAccounts } from "../api";
import { accountKeys } from "../keys";

export function useAccounts(includeInactive = false) {
  return useQuery({
    queryKey: accountKeys.list(includeInactive),
    queryFn: () => listAccounts(includeInactive),
  });
}
