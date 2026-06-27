import { Check, Shield } from "lucide-react";

interface BrandPanelProps {
  eyebrow: string;
  headline: string;
  lede: string;
  features: string[];
}

function LogoLight() {
  return (
    <svg
      width="27"
      height="27"
      viewBox="0 0 32 32"
      fill="none"
      style={{ flexShrink: 0 }}
    >
      <rect
        x="16"
        y="2.5"
        width="19"
        height="19"
        rx="3.5"
        transform="rotate(45 16 2.5)"
        fill="#fff"
        fillOpacity="0.97"
      />
      <rect
        x="16"
        y="8.9"
        width="10"
        height="10"
        rx="2"
        transform="rotate(45 16 8.9)"
        fill="none"
        stroke="#5E81AC"
        strokeWidth="1.6"
        strokeOpacity="0.9"
      />
      <line
        x1="16"
        y1="2.5"
        x2="16"
        y2="29.5"
        stroke="#5E81AC"
        strokeWidth="1.1"
        strokeOpacity="0.35"
      />
    </svg>
  );
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
          "radial-gradient(120% 80% at 12% 8%, rgba(136,192,208,.30) 0%, rgba(136,192,208,0) 42%), linear-gradient(157deg, #5E81AC 0%, #4A6791 38%, #3B4456 74%, #333A48 100%)",
      }}
    >
      {/* Diamond lattice overlay */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='72' height='72'%3E%3Cpath d='M36 3 69 36 36 69 3 36Z' fill='none' stroke='%23ffffff' stroke-opacity='0.045' stroke-width='1'/%3E%3C/svg%3E")`,
          backgroundSize: "72px 72px",
          maskImage: "linear-gradient(150deg, #000 0%, transparent 78%)",
          WebkitMaskImage: "linear-gradient(150deg, #000 0%, transparent 78%)",
        }}
      />

      {/* Large outline diamond */}
      <div
        className="pointer-events-none absolute"
        style={{
          right: -120,
          bottom: -130,
          width: 440,
          height: 440,
          transform: "rotate(45deg)",
          border: "1.5px solid rgba(255,255,255,0.10)",
          borderRadius: 56,
        }}
      />
      {/* Inner diamond */}
      <div
        className="pointer-events-none absolute"
        style={{
          right: -52,
          bottom: -62,
          width: 300,
          height: 300,
          transform: "rotate(45deg)",
          border: "1.5px solid rgba(255,255,255,0.07)",
          borderRadius: 40,
        }}
      />

      {/* Logo */}
      <div className="relative z-10 flex items-center gap-3">
        <LogoLight />
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
