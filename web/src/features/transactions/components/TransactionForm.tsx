"use client";

import { useEffect, useState } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle, X } from "lucide-react";
import { useForm } from "react-hook-form";

import { Badge } from "@/shared/components/ui/badge";
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
import { Label } from "@/shared/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/shared/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";
import { reaisInputToCentavos } from "@/shared/utils";

import { useAccountOptions } from "../hooks/useAccountOptions";
import { useCategoryOptions } from "../hooks/useCategoryOptions";
import { useCreateTransaction } from "../hooks/useCreateTransaction";
import {
  FREQUENCY_LABEL,
  PAYMENT_METHOD_LABEL,
  TRANSACTION_MODE_LABEL,
} from "../lib/labels";
import { transactionFormSchema, type TransactionFormValues } from "../schema";

import type { CreateTransactionCommand, TransactionFrequency } from "../types";

const TRANSACTION_MODES = ["SINGLE", "RECURRING", "TRANSFER"] as const;
const PAYMENT_METHODS = ["PIX", "TED", "DOC", "BOLETO", "CASH"] as const;
const FREQUENCIES = [
  "WEEKLY",
  "BIWEEKLY",
  "MONTHLY",
  "BIMONTHLY",
  "QUARTERLY",
  "SEMIANNUAL",
  "ANNUAL",
] as const;

function today(): string {
  return new Date().toLocaleDateString("en-CA");
}

const emptyDefaults: TransactionFormValues = {
  mode: "SINGLE",
  type: "DEBIT",
  amount: "",
  date: today(),
  description: "",
  categoryId: "",
  paymentMethod: "",
  accountId: "",
  destinationAccountId: "",
  frequency: "",
  endDate: "",
  notes: "",
  tags: [],
};

interface TransactionFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function TransactionForm({ open, onOpenChange }: TransactionFormProps) {
  const [serverError, setServerError] = useState<string | null>(null);
  const [tagInput, setTagInput] = useState("");
  const createTransaction = useCreateTransaction();
  const { data: accounts, refetch: refetchAccounts } = useAccountOptions();
  const { data: categories, refetch: refetchCategories } = useCategoryOptions();

  const form = useForm<TransactionFormValues>({
    resolver: zodResolver(transactionFormSchema),
    defaultValues: emptyDefaults,
  });

  useEffect(() => {
    if (open) {
      setServerError(null);
      setTagInput("");
      form.reset(emptyDefaults);
      // This feature can't invalidate features/account or features/category's
      // own query keys (cross-feature imports are forbidden), so its own
      // dropdown caches can go stale when an account/category is created
      // elsewhere. Refetch on every open rather than trust the cache.
      refetchAccounts();
      refetchCategories();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const mode = form.watch("mode");
  const accountId = form.watch("accountId");
  const tags = form.watch("tags");

  function addTag() {
    const value = tagInput.trim();
    if (!value || tags.includes(value)) {
      setTagInput("");
      return;
    }
    form.setValue("tags", [...tags, value]);
    setTagInput("");
  }

  function removeTag(tag: string) {
    form.setValue(
      "tags",
      tags.filter((t) => t !== tag),
    );
  }

  async function onSubmit(values: TransactionFormValues) {
    setServerError(null);
    try {
      const command = buildCommand(values);
      await createTransaction.mutateAsync(command);
      onOpenChange(false);
    } catch (err) {
      setServerError(
        err instanceof Error ? err.message : "Erro ao criar transação",
      );
    }
  }

  const isSubmitting = form.formState.isSubmitting;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Nova transação</DialogTitle>
          <DialogDescription>
            Registre uma movimentação única, recorrente ou uma transferência
            entre contas.
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
              name="mode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo de lançamento</FormLabel>
                  <FormControl>
                    <RadioGroup
                      value={field.value}
                      onValueChange={field.onChange}
                      className="grid-flow-col justify-start gap-6"
                    >
                      {TRANSACTION_MODES.map((value) => (
                        <label
                          key={value}
                          className="flex items-center gap-2 text-sm"
                        >
                          <RadioGroupItem value={value} />
                          {TRANSACTION_MODE_LABEL[value]}
                        </label>
                      ))}
                    </RadioGroup>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {mode !== "TRANSFER" && (
              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Natureza</FormLabel>
                    <FormControl>
                      <RadioGroup
                        value={field.value}
                        onValueChange={field.onChange}
                        className="grid-flow-col justify-start gap-6"
                      >
                        <label className="flex items-center gap-2 text-sm">
                          <RadioGroupItem value="DEBIT" />
                          Saída
                        </label>
                        <label className="flex items-center gap-2 text-sm">
                          <RadioGroupItem value="CREDIT" />
                          Entrada
                        </label>
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descrição</FormLabel>
                  <FormControl>
                    <Input placeholder="Ex: Supermercado" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="amount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Valor (R$)</FormLabel>
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

              <FormField
                control={form.control}
                name="date"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Data</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="accountId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>
                    {mode === "TRANSFER" ? "Conta de origem" : "Conta"}
                  </FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione a conta" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {accounts?.map((account) => (
                        <SelectItem key={account.id} value={account.id}>
                          {account.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {mode === "TRANSFER" && (
              <div className="flex flex-col gap-2">
                <Label>Destino</Label>
                <RadioGroup
                  defaultValue="account"
                  className="grid-flow-col justify-start gap-6"
                >
                  <label className="flex items-center gap-2 text-sm">
                    <RadioGroupItem value="account" />
                    Conta
                  </label>
                  <label className="text-muted-foreground flex items-center gap-2 text-sm">
                    <RadioGroupItem value="card" disabled />
                    Cartão
                    <span className="text-xs">
                      (em breve — aguardando funcionalidade de cartões)
                    </span>
                  </label>
                </RadioGroup>

                <FormField
                  control={form.control}
                  name="destinationAccountId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Conta de destino</FormLabel>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Selecione a conta de destino" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {accounts
                            ?.filter((account) => account.id !== accountId)
                            .map((account) => (
                              <SelectItem key={account.id} value={account.id}>
                                {account.name}
                              </SelectItem>
                            ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            )}

            {mode !== "TRANSFER" && (
              <FormField
                control={form.control}
                name="paymentMethod"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Forma de pagamento</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Selecione a forma de pagamento" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {PAYMENT_METHODS.map((value) => (
                          <SelectItem key={value} value={value}>
                            {PAYMENT_METHOD_LABEL[value]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}

            <FormField
              control={form.control}
              name="categoryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Categoria</FormLabel>
                  <Select
                    value={field.value || "none"}
                    onValueChange={(value) =>
                      field.onChange(value === "none" ? "" : value)
                    }
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Sem categoria" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="none">Sem categoria</SelectItem>
                      {categories?.map((category) => (
                        <SelectItem key={category.id} value={category.id}>
                          {category.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {mode === "RECURRING" && (
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="frequency"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Frequência</FormLabel>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Selecione a frequência" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {FREQUENCIES.map((value) => (
                            <SelectItem key={value} value={value}>
                              {FREQUENCY_LABEL[value]}
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
                  name="endDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Data final (opcional)</FormLabel>
                      <FormControl>
                        <Input type="date" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="transaction-tag-input">Tags</Label>
              <div className="flex gap-2">
                <Input
                  id="transaction-tag-input"
                  placeholder="Adicionar tag"
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addTag();
                    }
                  }}
                />
                <Button type="button" variant="outline" onClick={addTag}>
                  Adicionar
                </Button>
              </div>
              {tags.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {tags.map((tag) => (
                    <Badge key={tag} variant="secondary" className="gap-1">
                      {tag}
                      <button
                        type="button"
                        onClick={() => removeTag(tag)}
                        aria-label={`Remover tag ${tag}`}
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              )}
            </div>

            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Notas (opcional)</FormLabel>
                  <FormControl>
                    <Input placeholder="Observações" {...field} />
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

function buildCommand(values: TransactionFormValues): CreateTransactionCommand {
  const amount = reaisInputToCentavos(values.amount);

  if (values.mode === "TRANSFER") {
    return {
      mode: "TRANSFER",
      type: "DEBIT",
      amount,
      date: values.date,
      description: values.description,
      categoryId: values.categoryId || undefined,
      paymentMethod: "TRANSFER",
      accountId: values.accountId,
      tags: values.tags,
      notes: values.notes || undefined,
      transfer: { destinationAccountId: values.destinationAccountId! },
    };
  }

  const base: CreateTransactionCommand = {
    mode: values.mode,
    type: values.type,
    amount,
    date: values.date,
    description: values.description,
    categoryId: values.categoryId || undefined,
    paymentMethod: values.paymentMethod || undefined,
    accountId: values.accountId,
    tags: values.tags,
    notes: values.notes || undefined,
  };

  if (values.mode === "RECURRING") {
    return {
      ...base,
      recurring: {
        frequency: values.frequency as TransactionFrequency,
        endDate: values.endDate || undefined,
      },
    };
  }

  return base;
}
