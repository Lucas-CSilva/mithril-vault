"use client";

import Link from "next/link";
import { AlertTriangle, LayoutGrid, RefreshCw } from "lucide-react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="relative grid min-h-screen place-items-center overflow-hidden bg-[#ECEFF4] px-6 py-10">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "radial-gradient(90% 70% at 50% -10%, rgba(94,129,172,0.08) 0%, transparent 55%)",
        }}
      />
      <div className="pointer-events-none absolute top-[46%] left-1/2 h-[560px] w-[560px] -translate-x-1/2 -translate-y-1/2 rotate-45 rounded-[96px] border border-[#D8DEE9]/50" />
      <div className="pointer-events-none absolute top-[46%] left-1/2 h-[380px] w-[380px] -translate-x-1/2 -translate-y-1/2 rotate-45 rounded-[72px] border border-[#D8DEE9]" />

      <div className="relative z-10 w-full max-w-[460px] text-center">
        <div className="mb-10 inline-flex items-center gap-[10px]">
          <svg width={24} height={24} viewBox="0 0 32 32" fill="none">
            <rect
              x="16"
              y="2.5"
              width="19"
              height="19"
              rx="3.5"
              transform="rotate(45 16 2.5)"
              fill="#5E81AC"
            />
            <rect
              x="16"
              y="8.7"
              width="10.3"
              height="10.3"
              rx="2"
              transform="rotate(45 16 8.7)"
              fill="none"
              stroke="#fff"
              strokeWidth="1.5"
              strokeOpacity="0.85"
            />
            <line
              x1="16"
              y1="2.5"
              x2="16"
              y2="29.5"
              stroke="#fff"
              strokeWidth="1.2"
              strokeOpacity="0.35"
            />
          </svg>
          <span className="text-[15px] font-bold tracking-[-0.015em] whitespace-nowrap text-[#2E3440]">
            Mithril Vault
          </span>
        </div>

        <div className="mx-auto mb-[22px] grid h-[60px] w-[60px] place-items-center rounded-[18px] border border-[#D8DEE9] bg-[#E5E9F0] shadow-md">
          <AlertTriangle size={28} className="text-[#5E81AC]" />
        </div>

        <div className="inline-flex items-baseline font-mono text-[78px] leading-none font-bold tracking-[-0.04em] text-[#2E3440]">
          <span>5</span>
          <span className="text-[#5E81AC]">0</span>
          <span>0</span>
        </div>

        <div className="mt-[18px] font-mono text-[11px] font-semibold tracking-[0.18em] text-[#4C566A]/70 uppercase">
          Erro do servidor
        </div>

        <h1 className="mt-[10px] text-[25px] leading-tight font-bold tracking-[-0.02em] text-[#2E3440]">
          Algo deu errado do nosso lado.
        </h1>

        <p className="mx-auto mt-[11px] max-w-[380px] text-[14.5px] leading-[1.55] text-[#4C566A]">
          Encontramos um problema inesperado ao processar sua solicitação. Seus
          dados estão seguros — tente novamente em instantes.
        </p>

        <div className="mt-7 flex flex-wrap justify-center gap-[11px]">
          <button
            onClick={reset}
            className="inline-flex h-[44px] items-center gap-2 rounded-[10px] bg-[#5E81AC] px-[19px] text-[14px] font-semibold text-white shadow-sm transition-[filter] hover:brightness-110"
          >
            <RefreshCw size={16} />
            Tentar novamente
          </button>
          <Link
            href="/dashboard"
            className="inline-flex h-[44px] items-center gap-2 rounded-[10px] border border-[#D8DEE9] bg-[#E5E9F0] px-[18px] text-[14px] font-semibold text-[#3B4252] transition-colors hover:border-[#4C566A]/40 hover:bg-[#D8DEE9]"
          >
            <LayoutGrid size={16} />
            Ir para Visão Geral
          </Link>
        </div>

        {error.digest && (
          <div className="mt-[26px] font-mono text-[11px] tracking-[0.02em] text-[#4C566A]/60">
            Código 500
            <span className="mx-2 text-[#4C566A]/40">·</span>
            ref {error.digest}
          </div>
        )}
      </div>
    </div>
  );
}
