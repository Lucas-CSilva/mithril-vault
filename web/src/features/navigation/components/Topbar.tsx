"use client";

import { usePathname } from "next/navigation";

import { Bell, Calendar, ChevronDown, Menu, Plus, Search } from "lucide-react";

import { LogoutButton } from "./LogoutButton";
import { PAGE_META } from "../nav-items";

function currentMonthLabel(): string {
  const label = new Intl.DateTimeFormat("pt-BR", {
    month: "long",
    year: "numeric",
  }).format(new Date());
  return label.charAt(0).toUpperCase() + label.slice(1);
}

interface TopbarProps {
  onMenuClick: () => void;
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const pathname = usePathname();
  const meta = PAGE_META[pathname] ?? PAGE_META["/dashboard"];

  return (
    <header
      className="border-line sticky top-0 z-20 flex items-center gap-4 border-b px-[30px] py-[18px] backdrop-blur-md"
      style={{ background: "var(--topbar-bg)" }}
    >
      <button
        aria-label="Abrir menu"
        onClick={onMenuClick}
        className="text-[var(--ink-2)] lg:hidden"
      >
        <Menu size={22} />
      </button>

      <div className="min-w-0">
        <h1 className="text-foreground font-display text-[21px] leading-[1.1] font-bold tracking-[-0.02em] whitespace-nowrap">
          {meta.title}
        </h1>
        <div className="mt-[1px] overflow-hidden text-[12.5px] text-ellipsis whitespace-nowrap text-[var(--ink-3)]">
          {meta.sub}
        </div>
      </div>

      <div className="flex-1" />

      <div
        className="border-line hidden w-[230px] items-center gap-[9px] rounded-[11px] border px-[14px] py-[9px] text-[var(--ink-4)] md:flex"
        style={{ background: "var(--surface)" }}
      >
        <Search size={16} />
        <span className="text-[13.5px]">Buscar transações…</span>
        <span
          className="ml-auto rounded-[5px] px-[6px] py-[2px] font-mono text-[10px] text-[var(--ink-4)]"
          style={{ background: "var(--surface-3)" }}
        >
          ⌘K
        </span>
      </div>

      <button
        className="border-line hidden items-center gap-[9px] rounded-[11px] border px-[13px] py-[9px] text-[13.5px] font-semibold text-[var(--ink-2)] sm:flex"
        style={{ background: "var(--surface)" }}
      >
        <Calendar size={16} style={{ color: "var(--ink-4)" }} />
        {currentMonthLabel()}
        <ChevronDown size={15} style={{ color: "var(--ink-4)" }} />
      </button>

      <button
        aria-label="Notificações"
        className="border-line relative grid h-10 w-10 place-items-center rounded-[11px] border text-[var(--ink-2)]"
        style={{ background: "var(--surface)" }}
      >
        <Bell size={18} />
        <span
          className="absolute top-[9px] right-[10px] h-[7px] w-[7px] rounded-full"
          style={{
            background: "var(--neg)",
            border: "1.5px solid var(--surface)",
          }}
        />
      </button>

      <button
        className="hidden h-10 items-center gap-2 rounded-[11px] px-4 text-[13.5px] font-semibold text-white shadow-sm sm:flex"
        style={{ background: "var(--frost-deep)" }}
      >
        <Plus size={17} strokeWidth={2.4} />
        Adicionar
      </button>

      <LogoutButton />
    </header>
  );
}
