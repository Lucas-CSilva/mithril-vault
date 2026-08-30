import type {
  PaymentMethod,
  TransactionFrequency,
  TransactionMode,
} from "../types";

export const TRANSACTION_MODE_LABEL: Record<
  Extract<TransactionMode, "SINGLE" | "RECURRING" | "TRANSFER">,
  string
> = {
  SINGLE: "Única",
  RECURRING: "Recorrente",
  TRANSFER: "Transferência",
};

// DEBIT_CARD/CREDIT_CARD intentionally omitted — TransactionOriginResolver
// rejects them with 501 until the cards feature exists.
export const PAYMENT_METHOD_LABEL: Record<
  Extract<PaymentMethod, "PIX" | "TED" | "DOC" | "BOLETO" | "CASH">,
  string
> = {
  PIX: "Pix",
  TED: "TED",
  DOC: "DOC",
  BOLETO: "Boleto",
  CASH: "Dinheiro",
};

export const FREQUENCY_LABEL: Record<TransactionFrequency, string> = {
  WEEKLY: "Semanal",
  BIWEEKLY: "Quinzenal",
  MONTHLY: "Mensal",
  BIMONTHLY: "Bimestral",
  QUARTERLY: "Trimestral",
  SEMIANNUAL: "Semestral",
  ANNUAL: "Anual",
};
