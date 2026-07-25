import type { Centavos } from "@/shared/types";

export type AccountType = "CHECKING" | "SAVINGS" | "CASH" | "DIGITAL";

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  institution: string | null;
  initialBalance: Centavos;
  currentBalance: Centavos;
  color: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface CreateAccountCommand {
  name: string;
  type: AccountType;
  institution?: string;
  initialBalance: Centavos;
  color?: string;
}

export interface UpdateAccountCommand {
  name?: string;
  type?: AccountType;
  institution?: string;
  color?: string;
}

export type ReconciliationMethod =
  | "ADJUSTING_TRANSACTION"
  | "ADJUST_INITIAL_BALANCE";

export interface ReconcileAccountCommand {
  realBalance: Centavos;
  method: ReconciliationMethod;
}

export interface BalancePoint {
  date: string;
  balance: Centavos;
}

export interface BalanceHistoryResponse {
  accountId: string;
  points: BalancePoint[];
}
