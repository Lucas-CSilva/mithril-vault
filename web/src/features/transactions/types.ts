import type { Centavos } from "@/shared/types";

export type TransactionMode =
  | "SINGLE"
  | "RECURRING"
  | "INSTALLMENT"
  | "TRANSFER";

export type TransactionType = "DEBIT" | "CREDIT";

export type PaymentMethod =
  | "PIX"
  | "TED"
  | "DOC"
  | "DEBIT_CARD"
  | "CREDIT_CARD"
  | "BOLETO"
  | "CASH"
  | "TRANSFER";

export type TransactionFrequency =
  | "WEEKLY"
  | "BIWEEKLY"
  | "MONTHLY"
  | "BIMONTHLY"
  | "QUARTERLY"
  | "SEMIANNUAL"
  | "ANNUAL";

export interface RecurringConfig {
  frequency: TransactionFrequency;
  endDate?: string;
}

export interface TransferConfig {
  destinationAccountId: string;
  transferPairId?: string;
}

export interface CreateTransactionCommand {
  mode: TransactionMode;
  type: TransactionType;
  amount: Centavos;
  date: string;
  description: string;
  categoryId?: string;
  paymentMethod?: PaymentMethod;
  accountId?: string;
  cardId?: string;
  tags?: string[];
  notes?: string;
  recurring?: RecurringConfig;
  transfer?: TransferConfig;
}

export interface AccountSummary {
  id: string;
  name: string;
}

export interface CardSummary {
  id: string;
  name: string;
}

export interface InvoiceSummary {
  id: string;
}

export interface Transaction {
  id: string;
  type: TransactionType;
  amount: Centavos;
  date: string;
  description: string;
  categoryId: string | null;
  paymentMethod: PaymentMethod | null;
  accountId: string | null;
  invoiceId: string | null;
  account: AccountSummary | null;
  card: CardSummary | null;
  invoice: InvoiceSummary | null;
  tags: string[];
  notes: string | null;
  createdAt: string;
}

export interface ListTransactionsParams {
  accountId?: string;
  invoiceId?: string;
  categoryId?: string;
  type?: TransactionType;
  paymentMethod?: PaymentMethod;
  startDate?: string;
  endDate?: string;
  search?: string;
  page?: number;
  size?: number;
}

export interface TransactionPage {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AccountOption {
  id: string;
  name: string;
}

export interface CategoryOption {
  id: string;
  name: string;
}
