import { httpApiClient } from "@/core/services/HttpApiClient";

import type {
  Account,
  BalanceHistoryResponse,
  CreateAccountCommand,
  ReconcileAccountCommand,
  UpdateAccountCommand,
} from "./types";

export function listAccounts(includeInactive = false): Promise<Account[]> {
  return httpApiClient.get<Account[]>(
    `/accounts?includeInactive=${includeInactive}`,
  );
}

export function getAccount(id: string): Promise<Account> {
  return httpApiClient.get<Account>(`/accounts/${id}`);
}

export function createAccount(command: CreateAccountCommand): Promise<Account> {
  return httpApiClient.post<Account>("/accounts", command);
}

export function updateAccount(
  id: string,
  command: UpdateAccountCommand,
): Promise<Account> {
  return httpApiClient.patch<Account>(`/accounts/${id}`, command);
}

export function deactivateAccount(id: string): Promise<void> {
  return httpApiClient.delete<void>(`/accounts/${id}`);
}

export function reactivateAccount(id: string): Promise<Account> {
  return httpApiClient.post<Account>(`/accounts/${id}/reactivate`);
}

export function reconcileAccount(
  id: string,
  command: ReconcileAccountCommand,
): Promise<Account> {
  return httpApiClient.post<Account>(`/accounts/${id}/reconcile`, command);
}

export function getAccountBalanceHistory(
  id: string,
): Promise<BalanceHistoryResponse> {
  return httpApiClient.get<BalanceHistoryResponse>(
    `/accounts/${id}/balance-history`,
  );
}
