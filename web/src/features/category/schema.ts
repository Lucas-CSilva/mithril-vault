import { z } from "zod";

export const categorySchema = z.object({
  name: z
    .string()
    .min(1, "Nome obrigatório")
    .max(100, "Nome deve ter no máximo 100 caracteres"),
  parentId: z.string().uuid().optional().or(z.literal("")),
  icon: z.string().max(50).optional().or(z.literal("")),
  color: z
    .string()
    .regex(/^#[0-9A-Fa-f]{6}$/, "Cor inválida")
    .optional()
    .or(z.literal("")),
});

export type CategoryFormValues = z.infer<typeof categorySchema>;
