"use client";

import { useState } from "react";

import Link from "next/link";

import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle, Mail, User } from "lucide-react";
import { useForm, useWatch } from "react-hook-form";

import { useAuth } from "@/core/contexts/AuthContext";

import { registerSchema, type RegisterFormValues } from "../schema";
import { AuthField } from "./AuthField";
import { BrandPanel } from "./BrandPanel";
import { PasswordField } from "./PasswordField";

function scorePassword(pw: string): number {
  if (!pw) return 0;
  let s = 0;
  if (pw.length >= 8) s++;
  if (pw.length >= 12) s++;
  if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) s++;
  if (/\d/.test(pw) && /[^A-Za-z0-9]/.test(pw)) s++;
  return Math.min(s, 4);
}

const STRENGTH = [
  {
    label: "Muito fraca",
    color: "var(--destructive)",
    hint: "Use ao menos 8 caracteres",
  },
  {
    label: "Fraca",
    color: "var(--destructive)",
    hint: "Misture maiúsculas e minúsculas",
  },
  {
    label: "Razoável",
    color: "var(--warning)",
    hint: "Adicione números ou símbolos",
  },
  { label: "Boa", color: "var(--nord-13)", hint: "Quase lá" },
  { label: "Forte", color: "var(--success)", hint: "Senha sólida" },
];

function StrengthMeter({ value }: { value: string }) {
  const score = scorePassword(value);
  const meta = STRENGTH[score];
  return (
    <div className="mt-[9px]">
      <div className="grid grid-cols-4 gap-[5px]">
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className="h-1 rounded-full transition-all duration-300"
            style={{
              background: i < score ? meta.color : "var(--background)",
            }}
          />
        ))}
      </div>
      <div className="mt-[7px] flex justify-between font-mono text-[11.5px]">
        <span style={{ color: value ? meta.color : "var(--foreground-dim)" }}>
          {value ? meta.label : "Força da senha"}
        </span>
        {value && (
          <span className="text-foreground-dim font-sans">{meta.hint}</span>
        )}
      </div>
    </div>
  );
}

export function RegisterForm() {
  const { register: registerUser } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      displayName: "",
      email: "",
      password: "",
    },
  });

  const passwordValue = useWatch({ control, name: "password" });

  async function onSubmit(values: RegisterFormValues) {
    setServerError(null);
    try {
      await registerUser({
        displayName: values.displayName,
        email: values.email,
        password: values.password,
      });
    } catch (err) {
      setServerError(
        err instanceof Error ? err.message : "Erro ao criar conta",
      );
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.04fr_1fr]">
      <BrandPanel
        eyebrow="Comece grátis"
        headline="Organize toda a sua vida financeira hoje."
        lede="Crie sua conta em menos de um minuto. Cada usuário vê apenas os próprios dados — sempre isolados."
        features={[
          "Conecte contas, cartões e investimentos em um só lugar",
          "Acompanhe orçamentos e metas mês a mês",
          "Apenas R$ — entrada manual ou importação de extratos",
        ]}
      />

      <div className="bg-background flex flex-col overflow-y-auto px-[40px] py-[30px]">
        <div className="flex items-center justify-end gap-[7px]">
          <span className="text-foreground-subtle text-[13px]">
            Já tem conta?
          </span>
          <Link
            href="/login"
            className="border-ring-border bg-card text-ring hover:bg-ring-bg inline-flex items-center rounded-[10px] border px-[13px] py-2 text-[13px] font-semibold transition-colors"
          >
            Entrar
          </Link>
        </div>

        <div className="mx-auto my-auto w-full max-w-[388px] py-6">
          <div className="text-foreground-dim font-mono text-[10.5px] font-semibold tracking-[0.16em] uppercase">
            Criar conta
          </div>
          <h1 className="text-foreground mt-3 text-[28px] leading-[1.1] font-bold tracking-[-0.025em]">
            Crie sua conta
          </h1>
          <p className="text-foreground-subtle mt-2 text-[14.5px] leading-[1.5]">
            Leva menos de um minuto. Sem cartão de crédito.
          </p>

          <form
            onSubmit={handleSubmit(onSubmit)}
            noValidate
            className="mt-[26px] flex flex-col gap-4"
          >
            {serverError && (
              <div className="border-error-border bg-error-bg text-error-foreground flex items-start gap-[10px] rounded-[14px] border p-[12px_14px] text-[13px] leading-[1.4]">
                <AlertTriangle size={16} className="mt-[1px] flex-shrink-0" />
                <span>{serverError}</span>
              </div>
            )}

            <AuthField
              label="Nome"
              icon={<User size={18} />}
              error={errors.displayName?.message}
            >
              <input
                type="text"
                placeholder="Como podemos te chamar?"
                autoComplete="name"
                className="text-foreground placeholder:text-foreground-dim min-w-0 flex-1 bg-transparent px-[14px] py-[13px] text-[15px] outline-none"
                {...register("displayName")}
              />
            </AuthField>

            <AuthField
              label="E-mail"
              icon={<Mail size={18} />}
              error={errors.email?.message}
            >
              <input
                type="email"
                placeholder="voce@email.com"
                autoComplete="email"
                className="text-foreground placeholder:text-foreground-dim min-w-0 flex-1 bg-transparent px-[14px] py-[13px] text-[15px] outline-none"
                {...register("email")}
              />
            </AuthField>

            <div>
              <PasswordField
                label="Senha"
                autoComplete="new-password"
                placeholder="Mínimo de 8 caracteres"
                error={errors.password?.message}
                {...register("password")}
              />
              <StrengthMeter value={passwordValue} />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="bg-ring mt-1 flex h-[48px] w-full items-center justify-center gap-[9px] rounded-[14px] text-[15px] font-semibold text-white shadow-sm transition-all hover:brightness-110 disabled:cursor-default disabled:opacity-70"
            >
              {isSubmitting ? (
                <>
                  <span className="h-[17px] w-[17px] animate-spin rounded-full border-2 border-white/35 border-t-white" />
                  Criando conta…
                </>
              ) : (
                "Criar conta"
              )}
            </button>
          </form>

          <div className="text-foreground-subtle mt-[22px] text-center text-[13.5px]">
            Já tem uma conta?{" "}
            <Link
              href="/login"
              className="text-ring font-semibold underline-offset-[3px] hover:underline"
            >
              Entrar
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
