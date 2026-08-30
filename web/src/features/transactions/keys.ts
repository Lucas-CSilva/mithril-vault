import type { ListTransactionsParams } from "./types";

export const transactionKeys = {
  all: ["transactions"] as const,
  list: (params: ListTransactionsParams) =>
    ["transactions", "list", params] as const,
  accountOptions: ["transactions", "account-options"] as const,
  categoryOptions: ["transactions", "category-options"] as const,
};
