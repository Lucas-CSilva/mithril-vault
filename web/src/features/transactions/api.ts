import { httpApiClient } from "@/core/services/HttpApiClient";

import type {
  AccountOption,
  CategoryOption,
  CreateTransactionCommand,
  ListTransactionsParams,
  Transaction,
  TransactionPage,
} from "./types";

export function createTransaction(
  command: CreateTransactionCommand,
): Promise<Transaction[]> {
  return httpApiClient.post<Transaction[]>("/transactions", command);
}

export function listTransactions(
  params: ListTransactionsParams = {},
): Promise<TransactionPage> {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const qs = search.toString();
  return httpApiClient.get<TransactionPage>(
    `/transactions${qs ? `?${qs}` : ""}`,
  );
}

// These duplicate features/account and features/category's own list calls
// rather than importing from them, since eslint-plugin-boundaries forbids
// one feature importing another (see web/CLAUDE.md's import boundaries).
export function listAccountOptions(): Promise<AccountOption[]> {
  return httpApiClient.get<AccountOption[]>("/accounts?includeInactive=false");
}

export function listCategoryOptions(): Promise<CategoryOption[]> {
  return httpApiClient.get<CategoryOption[]>("/categories");
}
