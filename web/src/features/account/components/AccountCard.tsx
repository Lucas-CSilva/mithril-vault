"use client";

import { Pencil, RefreshCw, RotateCcw, XCircle } from "lucide-react";

import { Sparkline } from "@/shared/components/Sparkline";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { cn, formatBRL } from "@/shared/utils";

import { useAccountBalanceHistory } from "../hooks/useAccountBalanceHistory";
import { DEFAULT_ACCOUNT_COLOR } from "../lib/colors";
import { ACCOUNT_TYPE_LABEL } from "../lib/labels";

import type { Account } from "../types";

interface AccountCardProps {
  account: Account;
  onEdit: (account: Account) => void;
  onReconcile: (account: Account) => void;
  onDeactivate: (account: Account) => void;
  onReactivate: (account: Account) => void;
}

function initials(name: string): string {
  return name.trim().slice(0, 2).toUpperCase();
}

export function AccountCard({
  account,
  onEdit,
  onReconcile,
  onDeactivate,
  onReactivate,
}: AccountCardProps) {
  const { data: history } = useAccountBalanceHistory(account.id);
  const color = account.color ?? DEFAULT_ACCOUNT_COLOR;
  const sparkData = history?.points.map((p) => p.balance);

  return (
    <div className="border-line bg-card relative overflow-hidden rounded-xl border p-5 shadow-sm">
      <div
        className="absolute top-0 left-0 h-[3px] w-full"
        style={{ background: color }}
      />

      <div className="mb-4 flex items-center gap-3">
        <div
          className="grid h-9 w-9 shrink-0 place-items-center rounded-[10px] text-[13px] font-bold"
          style={{ background: `${color}22`, color }}
        >
          {initials(account.name)}
        </div>
        <div className="min-w-0 flex-1">
          <div className="text-foreground truncate text-sm font-semibold">
            {account.name}
          </div>
          <div className="truncate text-[11.5px] text-[var(--ink-4)]">
            {ACCOUNT_TYPE_LABEL[account.type]}
            {account.institution ? ` · ${account.institution}` : ""}
          </div>
        </div>
        {!account.isActive && <Badge variant="secondary">Inativa</Badge>}
      </div>

      <div className="eyebrow text-[9.5px]">Saldo atual</div>
      <div className="mt-1 flex items-end justify-between">
        <div
          className={cn(
            "font-mono text-[21px] font-bold tracking-tight",
            account.currentBalance < 0 && "text-[var(--neg)]",
          )}
        >
          {formatBRL(account.currentBalance)}
        </div>
        {sparkData && sparkData.length >= 2 && (
          <Sparkline data={sparkData} color={color} width={64} height={26} />
        )}
      </div>

      <div className="border-line mt-4 flex justify-end gap-1 border-t pt-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => onEdit(account)}
          aria-label={`Editar ${account.name}`}
        >
          <Pencil className="h-4 w-4" />
        </Button>
        {account.isActive ? (
          <>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => onReconcile(account)}
              aria-label={`Reconciliar ${account.name}`}
            >
              <RefreshCw className="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => onDeactivate(account)}
              aria-label={`Desativar ${account.name}`}
            >
              <XCircle className="h-4 w-4" />
            </Button>
          </>
        ) : (
          <Button
            variant="ghost"
            size="icon"
            onClick={() => onReactivate(account)}
            aria-label={`Reativar ${account.name}`}
          >
            <RotateCcw className="h-4 w-4" />
          </Button>
        )}
      </div>
    </div>
  );
}
