import { useQuery } from "@tanstack/react-query";

import { getAccountBalanceHistory } from "../api";
import { accountKeys } from "../keys";

export function useAccountBalanceHistory(id: string) {
  return useQuery({
    queryKey: accountKeys.balanceHistory(id),
    queryFn: () => getAccountBalanceHistory(id),
  });
}
