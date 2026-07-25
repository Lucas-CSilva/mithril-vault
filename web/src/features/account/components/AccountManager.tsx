"use client";

import { useState } from "react";

import { Plus } from "lucide-react";

import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/utils";

import { AccountForm } from "./AccountForm";
import { AccountList } from "./AccountList";
import { DeactivateAccountDialog } from "./DeactivateAccountDialog";
import { ReconcileAccountDialog } from "./ReconcileAccountDialog";
import { useReactivateAccount } from "../hooks/useReactivateAccount";

import type { Account } from "../types";

export function AccountManager() {
  const [includeInactive, setIncludeInactive] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<Account | undefined>(
    undefined,
  );
  const [deactivatingAccount, setDeactivatingAccount] =
    useState<Account | null>(null);
  const [reconcilingAccount, setReconcilingAccount] = useState<Account | null>(
    null,
  );
  const reactivateAccount = useReactivateAccount();

  function handleCreate() {
    setEditingAccount(undefined);
    setFormOpen(true);
  }

  function handleEdit(account: Account) {
    setEditingAccount(account);
    setFormOpen(true);
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-foreground text-2xl font-semibold">
            Minhas Contas
          </h1>
          <p className="text-muted-foreground text-sm">
            Gerencie suas contas correntes, poupanças e carteiras.
          </p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="h-4 w-4" />
          Nova conta
        </Button>
      </div>

      <div className="border-line bg-surface-2 flex gap-1 self-start rounded-lg border p-1">
        {[
          { key: false, label: "Ativas" },
          { key: true, label: "Todas" },
        ].map(({ key, label }) => (
          <button
            key={label}
            type="button"
            onClick={() => setIncludeInactive(key)}
            className={cn(
              "rounded-md px-3 py-1.5 text-[12.5px] font-semibold transition-colors",
              includeInactive === key
                ? "bg-surface text-frost-deep shadow-sm"
                : "text-[var(--ink-4)]",
            )}
          >
            {label}
          </button>
        ))}
      </div>

      <AccountList
        includeInactive={includeInactive}
        onEdit={handleEdit}
        onReconcile={setReconcilingAccount}
        onDeactivate={setDeactivatingAccount}
        onReactivate={(account) => reactivateAccount.mutate(account.id)}
      />

      <AccountForm
        open={formOpen}
        onOpenChange={setFormOpen}
        account={editingAccount}
      />

      <DeactivateAccountDialog
        account={deactivatingAccount}
        onOpenChange={(open) => {
          if (!open) setDeactivatingAccount(null);
        }}
      />

      <ReconcileAccountDialog
        account={reconcilingAccount}
        onOpenChange={(open) => {
          if (!open) setReconcilingAccount(null);
        }}
      />
    </div>
  );
}
