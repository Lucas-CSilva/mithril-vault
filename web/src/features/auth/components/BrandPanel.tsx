import { Check, Shield } from "lucide-react";

import { RingSealLogo } from "@/shared/components/RingSealLogo";

interface BrandPanelProps {
  eyebrow: string;
  headline: string;
  lede: string;
  features: string[];
}

export function BrandPanel({
  eyebrow,
  headline,
  lede,
  features,
}: BrandPanelProps) {
  return (
    <div
      className="relative hidden flex-col overflow-hidden px-[52px] pt-[46px] pb-[40px] text-white lg:flex"
      style={{
        background:
          "radial-gradient(120% 80% at 12% 8%, var(--brand-panel-haze) 0%, rgba(122,138,164,0) 42%), linear-gradient(157deg, var(--frost-deep) 0%, var(--frost-shade) 38%, var(--frost-dark) 74%, var(--ink) 100%)",
      }}
    >
      {/* Dot lattice overlay */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage:
            "radial-gradient(rgba(255,255,255,0.055) 1px, transparent 1.4px)",
          backgroundSize: "22px 22px",
          maskImage: "linear-gradient(150deg, #000 0%, transparent 80%)",
          WebkitMaskImage: "linear-gradient(150deg, #000 0%, transparent 80%)",
        }}
      />

      {/* Concentric ring motif, echoes the seal */}
      <div
        className="pointer-events-none absolute rounded-full"
        style={{
          right: -150,
          bottom: -160,
          width: 460,
          height: 460,
          border: "1.5px solid rgba(255,255,255,0.10)",
        }}
      />
      <div
        className="pointer-events-none absolute rounded-full"
        style={{
          right: -78,
          bottom: -88,
          width: 320,
          height: 320,
          border: "1.5px solid rgba(255,255,255,0.07)",
        }}
      />

      {/* Logo */}
      <div className="relative z-10 flex items-center gap-3">
        <RingSealLogo variant="light" size={27} />
        <div style={{ lineHeight: 1.04 }}>
          <div className="text-[17px] font-bold tracking-[-0.015em] whitespace-nowrap">
            Mithril Vault
          </div>
          <div
            className="mt-[3px] font-mono text-[9.5px] tracking-[0.18em] uppercase"
            style={{ color: "rgba(255,255,255,0.62)" }}
          >
            Finanças
          </div>
        </div>
      </div>

      {/* Brand body */}
      <div className="relative z-10 mt-auto max-w-[460px] pt-10">
        <div
          className="mb-[18px] font-mono text-[10.5px] font-semibold tracking-[0.2em] uppercase"
          style={{ color: "rgba(255,255,255,0.66)" }}
        >
          {eyebrow}
        </div>
        <h2
          className="text-[33px] leading-[1.12] font-bold tracking-[-0.025em]"
          style={{ textWrap: "balance" } as React.CSSProperties}
        >
          {headline}
        </h2>
        <p
          className="mt-4 max-w-[400px] text-[15px] leading-[1.55]"
          style={{ color: "rgba(255,255,255,0.78)" }}
        >
          {lede}
        </p>
        <div className="mt-[30px] flex flex-col gap-[13px]">
          {features.map((feat, i) => (
            <div
              key={i}
              className="flex items-start gap-[11px] text-[13.5px]"
              style={{ color: "rgba(255,255,255,0.86)" }}
            >
              <span
                className="mt-[1px] grid h-[21px] w-[21px] flex-shrink-0 place-items-center rounded-[7px] text-white"
                style={{ background: "rgba(255,255,255,0.13)" }}
              >
                <Check size={13} strokeWidth={2.6} />
              </span>
              <span>{feat}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Footer */}
      <div
        className="relative z-10 mt-11 flex items-center gap-[9px] text-[12px]"
        style={{ color: "rgba(255,255,255,0.62)" }}
      >
        <Shield size={15} />
        <span>Seus dados isolados por usuário — visíveis só para você.</span>
      </div>
    </div>
  );
}
