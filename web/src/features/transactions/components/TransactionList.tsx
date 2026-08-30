"use client";

import { AlertCircle } from "lucide-react";

import { ApiError } from "@/core/services/HttpApiClient";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/components/ui/table";
import { formatBRL, formatDate } from "@/shared/utils";

import { useTransactions } from "../hooks/useTransactions";

export function TransactionList() {
  const { data, isPending, isError, error } = useTransactions();

  if (isPending) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="bg-muted h-12 w-full animate-pulse rounded-md"
          />
        ))}
      </div>
    );
  }

  // GET /transactions isn't implemented yet — an unmapped method on an
  // already-mapped path (POST exists) yields 405, not 404.
  if (
    error instanceof ApiError &&
    (error.status === 404 || error.status === 405)
  ) {
    return (
      <div className="border-line bg-surface-2 flex flex-col items-center gap-2 rounded-md border border-dashed p-8 text-center">
        <AlertCircle className="text-muted-foreground h-5 w-5" />
        <p className="text-foreground text-sm font-medium">
          Listagem de transações ainda não disponível
        </p>
        <p className="text-muted-foreground text-sm">
          Esta funcionalidade está em construção no backend.
        </p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="border-destructive/30 bg-destructive/5 text-muted-foreground rounded-md border p-8 text-center text-sm">
        Não foi possível carregar as transações.
      </div>
    );
  }

  if (data.content.length === 0) {
    return (
      <div className="text-muted-foreground rounded-md border border-dashed p-8 text-center text-sm">
        Nenhuma transação encontrada.
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Data</TableHead>
          <TableHead>Descrição</TableHead>
          <TableHead>Conta</TableHead>
          <TableHead className="text-right">Valor</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.content.map((transaction) => (
          <TableRow key={transaction.id}>
            <TableCell>{formatDate(transaction.date)}</TableCell>
            <TableCell>{transaction.description}</TableCell>
            <TableCell>{transaction.account?.name ?? "—"}</TableCell>
            <TableCell className="text-right">
              {transaction.type === "DEBIT" ? "-" : "+"}
              {formatBRL(transaction.amount)}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
