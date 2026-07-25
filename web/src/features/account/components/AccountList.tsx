"use client";

import { Button } from "@/shared/components/ui/button";

import { AccountCard } from "./AccountCard";
import { useAccounts } from "../hooks/useAccounts";

import type { Account } from "../types";

interface AccountListProps {
  includeInactive: boolean;
  onEdit: (account: Account) => void;
  onReconcile: (account: Account) => void;
  onDeactivate: (account: Account) => void;
  onReactivate: (account: Account) => void;
}

export function AccountList({
  includeInactive,
  onEdit,
  onReconcile,
  onDeactivate,
  onReactivate,
}: AccountListProps) {
  const {
    data: accounts,
    isPending,
    isError,
    refetch,
  } = useAccounts(includeInactive);

  if (isPending) {
    return (
      <div className="grid grid-cols-[repeat(auto-fill,minmax(230px,1fr))] gap-4">
        {Array.from({ length: 3 }).map((_, index) => (
          <div
            key={index}
            className="bg-muted h-[136px] w-full animate-pulse rounded-xl"
          />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="border-destructive/30 bg-destructive/5 flex flex-col items-center gap-3 rounded-md border p-8 text-center">
        <p className="text-muted-foreground text-sm">
          Não foi possível carregar as contas.
        </p>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          Tentar novamente
        </Button>
      </div>
    );
  }

  if (accounts.length === 0) {
    return (
      <div className="text-muted-foreground rounded-md border border-dashed p-8 text-center text-sm">
        Nenhuma conta encontrada.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-[repeat(auto-fill,minmax(230px,1fr))] gap-4">
      {accounts.map((account) => (
        <AccountCard
          key={account.id}
          account={account}
          onEdit={onEdit}
          onReconcile={onReconcile}
          onDeactivate={onDeactivate}
          onReactivate={onReactivate}
        />
      ))}
    </div>
  );
}
