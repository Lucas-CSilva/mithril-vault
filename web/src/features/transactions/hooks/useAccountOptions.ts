import { useQuery } from "@tanstack/react-query";

import { listAccountOptions } from "../api";
import { transactionKeys } from "../keys";

export function useAccountOptions() {
  return useQuery({
    queryKey: transactionKeys.accountOptions,
    queryFn: listAccountOptions,
  });
}
