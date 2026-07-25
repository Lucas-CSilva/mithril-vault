import { z } from "zod";

const moneyInput = z
  .string()
  .min(1, "Valor obrigatório")
  .regex(/^-?\d+(,\d{1,2})?$/, "Valor inválido");

export const accountTypeSchema = z.enum([
  "CHECKING",
  "SAVINGS",
  "CASH",
  "DIGITAL",
]);

export const accountFormSchema = z.object({
  name: z
    .string()
    .min(1, "Nome obrigatório")
    .max(100, "Nome deve ter no máximo 100 caracteres"),
  type: accountTypeSchema,
  institution: z.string().max(100).optional().or(z.literal("")),
  initialBalance: moneyInput,
  color: z
    .string()
    .regex(/^#[0-9A-Fa-f]{6}$/, "Cor inválida")
    .optional()
    .or(z.literal("")),
});

export type AccountFormValues = z.infer<typeof accountFormSchema>;

export const reconcileFormSchema = z.object({
  realBalance: moneyInput,
});

export type ReconcileFormValues = z.infer<typeof reconcileFormSchema>;
