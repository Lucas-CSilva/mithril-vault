"use client";

import Link from "next/link";

import { AlertTriangle, LayoutGrid, RefreshCw } from "lucide-react";

import { RingSealLogo } from "@/shared/components/RingSealLogo";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="bg-background relative grid min-h-screen place-items-center overflow-hidden px-6 py-10">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "radial-gradient(90% 70% at 50% -10%, var(--accent-bg) 0%, transparent 55%)",
        }}
      />
      <div
        className="pointer-events-none absolute top-[46%] left-1/2 h-[560px] w-[560px] -translate-x-1/2 -translate-y-1/2 rounded-full border opacity-50"
        style={{ borderColor: "var(--line-2)", borderWidth: 1.5 }}
      />
      <div
        className="pointer-events-none absolute top-[46%] left-1/2 h-[380px] w-[380px] -translate-x-1/2 -translate-y-1/2 rounded-full border opacity-50"
        style={{ borderColor: "var(--line)", borderWidth: 1.5 }}
      />

      <div className="relative z-10 w-full max-w-[460px] text-center">
        <div className="mb-10 inline-flex items-center gap-[10px]">
          <RingSealLogo variant="dark" size={24} />
          <span className="text-foreground text-[15px] font-bold tracking-[-0.015em] whitespace-nowrap">
            Mithril Vault
          </span>
        </div>

        <div className="border-border bg-secondary mx-auto mb-[22px] grid h-[60px] w-[60px] place-items-center rounded-[18px] border shadow-md">
          <AlertTriangle size={28} className="text-frost-deep" />
        </div>

        <div className="text-foreground inline-flex items-baseline font-mono text-[78px] leading-none font-bold tracking-[-0.04em]">
          <span>5</span>
          <span className="text-frost-deep">0</span>
          <span>0</span>
        </div>

        <div className="text-muted-foreground/70 mt-[18px] font-mono text-[11px] font-semibold tracking-[0.18em] uppercase">
          Erro do servidor
        </div>

        <h1 className="text-foreground mt-[10px] text-[25px] leading-tight font-bold tracking-[-0.02em]">
          Algo deu errado do nosso lado.
        </h1>

        <p className="text-muted-foreground mx-auto mt-[11px] max-w-[380px] text-[14.5px] leading-[1.55]">
          Encontramos um problema inesperado ao processar sua solicitação. Seus
          dados estão seguros — tente novamente em instantes.
        </p>

        <div className="mt-7 flex flex-wrap justify-center gap-[11px]">
          <button
            onClick={reset}
            className="bg-frost-deep inline-flex h-[44px] items-center gap-2 rounded-[10px] px-[19px] text-[14px] font-semibold text-white shadow-sm transition-[filter] hover:brightness-110"
          >
            <RefreshCw size={16} />
            Tentar novamente
          </button>
          <Link
            href="/dashboard"
            className="border-border bg-secondary text-foreground-2 hover:border-muted-foreground/40 hover:bg-input inline-flex h-[44px] items-center gap-2 rounded-[10px] border px-[18px] text-[14px] font-semibold transition-colors"
          >
            <LayoutGrid size={16} />
            Ir para Visão Geral
          </Link>
        </div>

        {error.digest && (
          <div className="text-muted-foreground/60 mt-[26px] font-mono text-[11px] tracking-[0.02em]">
            Código 500
            <span className="text-muted-foreground/40 mx-2">·</span>
            ref {error.digest}
          </div>
        )}
      </div>
    </div>
  );
}
