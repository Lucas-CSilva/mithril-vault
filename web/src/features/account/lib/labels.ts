import type { AccountType, ReconciliationMethod } from "../types";

export const ACCOUNT_TYPE_LABEL: Record<AccountType, string> = {
  CHECKING: "Conta Corrente",
  SAVINGS: "Poupança",
  CASH: "Dinheiro",
  DIGITAL: "Conta Digital",
};

export const RECONCILIATION_METHOD_LABEL: Record<ReconciliationMethod, string> =
  {
    ADJUSTING_TRANSACTION: "Lançamento de ajuste",
    ADJUST_INITIAL_BALANCE: "Ajustar saldo inicial",
  };
