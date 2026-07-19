"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { Settings } from "lucide-react";

import { useAuth } from "@/core/contexts/AuthContext";
import { RingSealLogo } from "@/shared/components/RingSealLogo";

import { NAV_ITEMS } from "../nav-items";

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? "") : "";
  return (first + last).toUpperCase();
}

interface SidebarProps {
  onNavigate?: () => void;
}

export function Sidebar({ onNavigate }: SidebarProps) {
  const pathname = usePathname();
  const { user } = useAuth();

  return (
    <aside className="border-line bg-surface flex h-full w-[var(--sidebar-w)] flex-shrink-0 flex-col border-r px-[18px] py-[22px]">
      <div className="px-[6px] pb-6">
        <div className="flex items-center gap-[11px]">
          <RingSealLogo variant="dark" size={26} />
          <div style={{ lineHeight: 1.05 }}>
            <div className="text-foreground font-display text-[16px] font-semibold whitespace-nowrap">
              Mithril Vault
            </div>
            <div className="font-mono text-[9px] tracking-[0.16em] text-[var(--ink-4)] uppercase">
              Finanças pessoais
            </div>
          </div>
        </div>
      </div>

      <nav className="flex flex-1 flex-col gap-1">
        <div className="eyebrow px-3 pt-1 pb-2">Menu</div>
        {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              onClick={onNavigate}
              className="relative flex items-center gap-3 rounded-[11px] px-3 py-[9px] text-sm font-medium transition-colors"
              style={{
                color: active ? "var(--frost-deep)" : "var(--ink-3)",
                background: active ? "var(--accent-bg)" : "transparent",
                fontWeight: active ? 600 : 500,
              }}
            >
              {active && (
                <span
                  className="absolute top-1/2 h-[18px] w-[3px] -translate-y-1/2 rounded-full"
                  style={{ left: -12, background: "var(--frost-deep)" }}
                />
              )}
              <Icon size={19} strokeWidth={active ? 2.3 : 2} />
              <span>{label}</span>
            </Link>
          );
        })}
      </nav>

      {user && (
        <div className="border-line mt-auto border-t pt-4">
          <div className="flex w-full items-center gap-[10px] rounded-xl p-2">
            <div
              className="grid h-[34px] w-[34px] flex-shrink-0 place-items-center rounded-[10px] text-[13px] font-bold text-white"
              style={{
                background:
                  "linear-gradient(135deg, var(--frost-soft), var(--frost-deep))",
              }}
            >
              {initials(user.displayName)}
            </div>
            <div className="min-w-0 flex-1 text-left">
              <div className="text-foreground overflow-hidden text-[13px] font-semibold text-ellipsis whitespace-nowrap">
                {user.displayName}
              </div>
              <div className="overflow-hidden font-mono text-[10px] text-ellipsis whitespace-nowrap text-[var(--ink-4)]">
                {user.email}
              </div>
            </div>
            <Settings size={16} style={{ color: "var(--ink-4)" }} />
          </div>
        </div>
      )}
    </aside>
  );
}
