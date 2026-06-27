"use client";

import { useState } from "react";

import Link from "next/link";

import { zodResolver } from "@hookform/resolvers/zod";
import {
  AlertCircle,
  AlertTriangle,
  Eye,
  EyeOff,
  Lock,
  Mail,
} from "lucide-react";
import { useForm } from "react-hook-form";

import { useAuth } from "@/core/contexts/AuthContext";
import { cn } from "@/shared/utils/cn";

import { loginSchema, type LoginFormValues } from "../schema";
import { BrandPanel } from "./BrandPanel";

function AuthField({
  label,
  icon,
  error,
  children,
}: {
  label: string;
  icon: React.ReactNode;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-[7px]">
      <label className="text-foreground-2 text-[12.5px] font-semibold tracking-[-0.005em]">
        {label}
      </label>
      <div
        className={cn(
          "group bg-card relative flex items-center rounded-[14px] border transition-all duration-150",
          error
            ? "border-destructive focus-within:shadow-[0_0_0_3.5px_var(--error-bg)]"
            : "border-input hover:border-border-strong focus-within:border-ring focus-within:shadow-[0_0_0_3.5px_var(--ring-bg)]",
        )}
      >
        <span className="text-foreground-dim group-focus-within:text-ring ml-[14px] flex-shrink-0 transition-colors">
          {icon}
        </span>
        {children}
      </div>
      {error && (
        <p className="text-error-foreground flex items-center gap-[5px] text-[12px]">
          <AlertCircle size={13} className="flex-shrink-0" />
          {error}
        </p>
      )}
    </div>
  );
}

function PasswordField({
  label,
  error,
  autoComplete,
  ...registration
}: {
  label: string;
  error?: string;
  autoComplete?: string;
} & React.InputHTMLAttributes<HTMLInputElement>) {
  const [show, setShow] = useState(false);
  return (
    <AuthField label={label} icon={<Lock size={18} />} error={error}>
      <input
        type={show ? "text" : "password"}
        autoComplete={autoComplete}
        placeholder="••••••••"
        className="text-foreground placeholder:text-foreground-dim min-w-0 flex-1 bg-transparent px-[14px] py-[13px] text-[15px] outline-none"
        {...registration}
      />
      <button
        type="button"
        tabIndex={-1}
        aria-label={show ? "Ocultar senha" : "Mostrar senha"}
        onClick={() => setShow((s) => !s)}
        className="text-foreground-dim hover:bg-background hover:text-foreground-2 mr-1 grid h-[40px] w-[40px] flex-shrink-0 place-items-center rounded-[9px] transition-colors"
      >
        {show ? <EyeOff size={17} /> : <Eye size={17} />}
      </button>
    </AuthField>
  );
}

export function LoginForm() {
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const [remember, setRemember] = useState(true);

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
        {/* Topbar */}
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

        {/* Auth card */}
        <div className="mx-auto w-full max-w-[388px] pt-2 pb-6">
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
              error={errors.password?.message}
              {...register("password")}
            />

            {/* Remember + forgot */}
            <div className="flex items-center justify-between">
              <label className="inline-flex cursor-pointer items-center gap-[9px] select-none">
                <span
                  className={cn(
                    "grid h-[18px] w-[18px] flex-shrink-0 place-items-center rounded-[6px] border-[1.5px] transition-colors",
                    remember
                      ? "border-ring bg-ring text-white"
                      : "border-border-strong bg-card text-transparent",
                  )}
                  onClick={() => setRemember((r) => !r)}
                >
                  <svg
                    width="12"
                    height="12"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="3"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </span>
                <input
                  type="checkbox"
                  className="sr-only"
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                />
                <span className="text-foreground-2 text-[13px]">
                  Lembrar de mim
                </span>
              </label>
              <button
                type="button"
                className="text-ring text-[13px] font-semibold underline-offset-[3px] hover:underline"
              >
                Esqueceu a senha?
              </button>
            </div>

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

          <p className="text-foreground-dim mx-auto mt-[18px] max-w-[340px] text-center text-[11.5px] leading-[1.5]">
            Protegido por autenticação JWT. Ao entrar você concorda com os{" "}
            <a
              href="#"
              className="text-foreground-subtle underline underline-offset-[2px]"
            >
              Termos
            </a>{" "}
            e a{" "}
            <a
              href="#"
              className="text-foreground-subtle underline underline-offset-[2px]"
            >
              Política de Privacidade
            </a>
            .
          </p>
        </div>
      </div>
    </div>
  );
}
