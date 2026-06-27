import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().email("Email inválido"),
  password: z.string().min(8, "Senha deve ter ao menos 8 caracteres"),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export const registerSchema = z.object({
  displayName: z.string().min(1, "Nome obrigatório"),
  email: z.string().email("Email inválido"),
  password: z.string().min(8, "Senha deve ter ao menos 8 caracteres"),
  terms: z
    .boolean()
    .refine((v) => v === true, {
      message: "É preciso aceitar para continuar.",
    }),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;
