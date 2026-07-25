"use client";

import { useEffect, useState } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle } from "lucide-react";
import { useForm } from "react-hook-form";

import { Button } from "@/shared/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/shared/components/ui/form";
import { Input } from "@/shared/components/ui/input";
import { reaisInputToCentavos } from "@/shared/utils";

import { useReconcileAccount } from "../hooks/useReconcileAccount";
import { reconcileFormSchema, type ReconcileFormValues } from "../schema";

import type { Account } from "../types";

interface ReconcileAccountDialogProps {
  account: Account | null;
  onOpenChange: (open: boolean) => void;
}

export function ReconcileAccountDialog({
  account,
  onOpenChange,
}: ReconcileAccountDialogProps) {
  const [serverError, setServerError] = useState<string | null>(null);
  const reconcileAccount = useReconcileAccount();

  const form = useForm<ReconcileFormValues>({
    resolver: zodResolver(reconcileFormSchema),
    defaultValues: { realBalance: "" },
  });

  useEffect(() => {
    if (account) {
      setServerError(null);
      form.reset({ realBalance: "" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [account]);

  async function onSubmit(values: ReconcileFormValues) {
    if (!account) return;
    setServerError(null);
    try {
      await reconcileAccount.mutateAsync({
        id: account.id,
        command: {
          realBalance: reaisInputToCentavos(values.realBalance),
          method: "ADJUST_INITIAL_BALANCE",
        },
      });
      onOpenChange(false);
    } catch (err) {
      setServerError(
        err instanceof Error ? err.message : "Erro ao reconciliar conta",
      );
    }
  }

  const isSubmitting = form.formState.isSubmitting;

  return (
    <Dialog open={account !== null} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Reconciliar conta</DialogTitle>
          <DialogDescription>
            Informe o saldo real de &ldquo;{account?.name}&rdquo; conforme o
            extrato do banco. O saldo inicial da conta será ajustado para que o
            saldo atual passe a corresponder a esse valor.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            noValidate
            className="flex flex-col gap-4"
          >
            {serverError && (
              <div className="border-destructive/30 bg-destructive/5 text-destructive flex items-start gap-2 rounded-md border p-3 text-sm">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{serverError}</span>
              </div>
            )}

            <FormField
              control={form.control}
              name="realBalance"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Saldo real (R$)</FormLabel>
                  <FormControl>
                    <Input inputMode="decimal" placeholder="0,00" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Reconciliando…" : "Reconciliar"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
