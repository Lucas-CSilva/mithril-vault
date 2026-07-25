"use client";

import { useState } from "react";

import { Button } from "@/shared/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/components/ui/dialog";

import { useDeactivateAccount } from "../hooks/useDeactivateAccount";

import type { Account } from "../types";

interface DeactivateAccountDialogProps {
  account: Account | null;
  onOpenChange: (open: boolean) => void;
}

export function DeactivateAccountDialog({
  account,
  onOpenChange,
}: DeactivateAccountDialogProps) {
  const [serverError, setServerError] = useState<string | null>(null);
  const deactivateAccount = useDeactivateAccount();

  async function handleConfirm() {
    if (!account) return;
    setServerError(null);
    try {
      await deactivateAccount.mutateAsync(account.id);
      onOpenChange(false);
    } catch (err) {
      setServerError(
        err instanceof Error ? err.message : "Erro ao desativar conta",
      );
    }
  }

  return (
    <Dialog open={account !== null} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Desativar conta</DialogTitle>
          <DialogDescription>
            Tem certeza que deseja desativar &ldquo;{account?.name}&rdquo;? A
            conta e todas as suas transações serão preservadas, mas ficarão
            ocultas por padrão.
          </DialogDescription>
        </DialogHeader>

        {serverError && (
          <div className="border-destructive/30 bg-destructive/5 text-destructive rounded-md border p-3 text-sm">
            {serverError}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button
            variant="destructive"
            disabled={deactivateAccount.isPending}
            onClick={handleConfirm}
          >
            {deactivateAccount.isPending ? "Desativando…" : "Desativar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
