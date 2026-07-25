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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";
import { centavosToReaisInput, reaisInputToCentavos } from "@/shared/utils";

import { ColorPicker } from "./ColorPicker";
import { useCreateAccount } from "../hooks/useCreateAccount";
import { useUpdateAccount } from "../hooks/useUpdateAccount";
import { ACCOUNT_TYPE_LABEL } from "../lib/labels";
import { accountFormSchema, type AccountFormValues } from "../schema";

import type { Account, AccountType } from "../types";

const ACCOUNT_TYPES: AccountType[] = ["CHECKING", "SAVINGS", "CASH", "DIGITAL"];

interface AccountFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  account?: Account;
}

export function AccountForm({ open, onOpenChange, account }: AccountFormProps) {
  const isEditing = Boolean(account);
  const [serverError, setServerError] = useState<string | null>(null);
  const createAccount = useCreateAccount();
  const updateAccount = useUpdateAccount();

  const form = useForm<AccountFormValues>({
    resolver: zodResolver(accountFormSchema),
    defaultValues: {
      name: account?.name ?? "",
      type: account?.type ?? "CHECKING",
      institution: account?.institution ?? "",
      initialBalance: account
        ? centavosToReaisInput(account.initialBalance)
        : "",
      color: account?.color ?? "",
    },
  });

  useEffect(() => {
    if (open) {
      setServerError(null);
      form.reset({
        name: account?.name ?? "",
        type: account?.type ?? "CHECKING",
        institution: account?.institution ?? "",
        initialBalance: account
          ? centavosToReaisInput(account.initialBalance)
          : "",
        color: account?.color ?? "",
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, account]);

  async function onSubmit(values: AccountFormValues) {
    setServerError(null);
    try {
      if (isEditing && account) {
        await updateAccount.mutateAsync({
          id: account.id,
          command: {
            name: values.name,
            type: values.type,
            institution: values.institution || undefined,
            color: values.color || undefined,
          },
        });
      } else {
        await createAccount.mutateAsync({
          name: values.name,
          type: values.type,
          institution: values.institution || undefined,
          initialBalance: reaisInputToCentavos(values.initialBalance),
          color: values.color || undefined,
        });
      }
      onOpenChange(false);
    } catch (err) {
      setServerError(
        err instanceof Error ? err.message : "Erro ao salvar conta",
      );
    }
  }

  const isSubmitting = form.formState.isSubmitting;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar conta" : "Nova conta"}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Atualize o nome, tipo, instituição ou cor desta conta."
              : "Defina o nome, tipo e saldo inicial da nova conta."}
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
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nome</FormLabel>
                  <FormControl>
                    <Input placeholder="Ex: Nubank" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione o tipo" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {ACCOUNT_TYPES.map((type) => (
                        <SelectItem key={type} value={type}>
                          {ACCOUNT_TYPE_LABEL[type]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="institution"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Instituição</FormLabel>
                  <FormControl>
                    <Input placeholder="Ex: Nubank" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {!isEditing && (
              <FormField
                control={form.control}
                name="initialBalance"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Saldo inicial (R$)</FormLabel>
                    <FormControl>
                      <Input
                        inputMode="decimal"
                        placeholder="0,00"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}

            <FormField
              control={form.control}
              name="color"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Cor</FormLabel>
                  <FormControl>
                    <ColorPicker
                      value={field.value || undefined}
                      onChange={field.onChange}
                    />
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
                {isSubmitting ? "Salvando…" : "Salvar"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
