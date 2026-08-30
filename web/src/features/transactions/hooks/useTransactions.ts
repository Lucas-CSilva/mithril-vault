import { useQuery } from "@tanstack/react-query";

import { listTransactions } from "../api";
import { transactionKeys } from "../keys";

import type { ListTransactionsParams } from "../types";

export function useTransactions(params: ListTransactionsParams = {}) {
  return useQuery({
    queryKey: transactionKeys.list(params),
    queryFn: () => listTransactions(params),
    // GET /transactions 405s (path exists for POST, method doesn't) until
    // backend tasks 006-009 land — don't retry a hard failure.
    retry: false,
  });
}
