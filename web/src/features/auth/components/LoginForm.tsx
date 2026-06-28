"use client";

import { useState } from "react";

import Link from "next/link";

import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle, Mail } from "lucide-react";
import { useForm } from "react-hook-form";

import { useAuth } from "@/core/contexts/AuthContext";

import { loginSchema, type LoginFormValues } from "../schema";
import { AuthField } from "./AuthField";
import { BrandPanel } from "./BrandPanel";
import { PasswordField } from "./PasswordField";

export function LoginForm() {
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });

  async function onSubmit(values: LoginFormValues) {
    setServerError(null);
    try {
      await login(values);
    } catch (err) {
      setServerError(err instanceof Error ? err.message : "Erro ao entrar");
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.04fr_1fr]">
      <BrandPanel
        eyebrow="Finanças pessoais"
        headline="Seu dinheiro, com a clareza de um cofre."
        lede="Saldo, faturas, metas e renda fixa — tudo em um só painel, sempre em tempo real."
        features={[
          "Saldo líquido honesto, já descontando faturas em aberto",
          "Orçamentos, cofres de metas e carteira de renda fixa",
          "Importe extratos em CSV e OFX com deduplicação automática",
        ]}
      />

      <div className="bg-background flex flex-col overflow-y-auto px-[40px] py-[30px]">
        <div className="flex items-center justify-end gap-[7px]">
          <span className="text-foreground-subtle text-[13px]">
            Novo por aqui?
          </span>
          <Link
            href="/register"
            className="border-ring-border bg-card text-ring hover:bg-ring-bg inline-flex items-center rounded-[10px] border px-[13px] py-2 text-[13px] font-semibold transition-colors"
          >
            Criar conta
          </Link>
        </div>

        <div className="mx-auto my-auto w-full max-w-[388px] py-6">
          <div className="text-foreground-dim font-mono text-[10.5px] font-semibold tracking-[0.16em] uppercase">
            Acesse sua conta
          </div>
          <h1 className="text-foreground mt-3 text-[28px] leading-[1.1] font-bold tracking-[-0.025em]">
            Entrar
          </h1>
          <p className="text-foreground-subtle mt-2 text-[14.5px] leading-[1.5]">
            Bem-vindo de volta. Entre para continuar.
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

            <PasswordField
              label="Senha"
              autoComplete="current-password"
              placeholder="••••••••"
              error={errors.password?.message}
              {...register("password")}
            />

            <button
              type="submit"
              disabled={isSubmitting}
              className="bg-ring mt-1 flex h-[48px] w-full items-center justify-center gap-[9px] rounded-[14px] text-[15px] font-semibold text-white shadow-sm transition-all hover:brightness-110 disabled:cursor-default disabled:opacity-70"
            >
              {isSubmitting ? (
                <>
                  <span className="h-[17px] w-[17px] animate-spin rounded-full border-2 border-white/35 border-t-white" />
                  Entrando…
                </>
              ) : (
                "Entrar"
              )}
            </button>
          </form>

          <div className="text-foreground-subtle mt-[22px] text-center text-[13.5px]">
            Não tem uma conta?{" "}
            <Link
              href="/register"
              className="text-ring font-semibold underline-offset-[3px] hover:underline"
            >
              Criar conta gratuita
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
