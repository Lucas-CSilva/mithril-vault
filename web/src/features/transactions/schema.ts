import { z } from "zod";

const moneyInput = z
  .string()
  .min(1, "Valor obrigatório")
  .regex(/^-?\d+(,\d{1,2})?$/, "Valor inválido");

export const transactionModeSchema = z.enum([
  "SINGLE",
  "RECURRING",
  "TRANSFER",
]);

export const transactionTypeSchema = z.enum(["DEBIT", "CREDIT"]);

// DEBIT_CARD/CREDIT_CARD intentionally excluded — always 501 today (no cards feature).
export const paymentMethodSchema = z.enum([
  "PIX",
  "TED",
  "DOC",
  "BOLETO",
  "CASH",
]);

export const frequencySchema = z.enum([
  "WEEKLY",
  "BIWEEKLY",
  "MONTHLY",
  "BIMONTHLY",
  "QUARTERLY",
  "SEMIANNUAL",
  "ANNUAL",
]);

export const transactionFormSchema = z
  .object({
    mode: transactionModeSchema,
    type: transactionTypeSchema,
    amount: moneyInput,
    date: z.string().min(1, "Data obrigatória"),
    description: z
      .string()
      .min(1, "Descrição obrigatória")
      .max(200, "Descrição deve ter no máximo 200 caracteres"),
    categoryId: z.string().optional().or(z.literal("")),
    paymentMethod: paymentMethodSchema.optional().or(z.literal("")),
    accountId: z.string().optional().or(z.literal("")),
    destinationAccountId: z.string().optional().or(z.literal("")),
    frequency: frequencySchema.optional().or(z.literal("")),
    endDate: z.string().optional().or(z.literal("")),
    notes: z
      .string()
      .max(500, "Notas devem ter no máximo 500 caracteres")
      .optional()
      .or(z.literal("")),
    tags: z.array(z.string().max(50, "Tag deve ter no máximo 50 caracteres")),
  })
  .superRefine((values, ctx) => {
    if (values.mode !== "TRANSFER") {
      if (!values.accountId) {
        ctx.addIssue({
          code: "custom",
          message: "Conta obrigatória",
          path: ["accountId"],
        });
      }
      if (!values.paymentMethod) {
        ctx.addIssue({
          code: "custom",
          message: "Forma de pagamento obrigatória",
          path: ["paymentMethod"],
        });
      }
    }

    if (values.mode === "RECURRING") {
      if (!values.frequency) {
        ctx.addIssue({
          code: "custom",
          message: "Frequência obrigatória",
          path: ["frequency"],
        });
      }
      if (values.endDate && values.date && values.endDate < values.date) {
        ctx.addIssue({
          code: "custom",
          message: "Data final não pode ser antes da data da transação",
          path: ["endDate"],
        });
      }
    }

    if (values.mode === "TRANSFER") {
      if (!values.accountId) {
        ctx.addIssue({
          code: "custom",
          message: "Conta de origem obrigatória",
          path: ["accountId"],
        });
      }
      if (!values.destinationAccountId) {
        ctx.addIssue({
          code: "custom",
          message: "Conta de destino obrigatória",
          path: ["destinationAccountId"],
        });
      } else if (values.accountId === values.destinationAccountId) {
        ctx.addIssue({
          code: "custom",
          message: "Conta de destino deve ser diferente da conta de origem",
          path: ["destinationAccountId"],
        });
      }
    }
  });

export type TransactionFormValues = z.infer<typeof transactionFormSchema>;
