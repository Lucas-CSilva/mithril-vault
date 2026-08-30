"use client";

import { useState } from "react";

import { Plus } from "lucide-react";

import { Button } from "@/shared/components/ui/button";

import { TransactionForm } from "./TransactionForm";
import { TransactionList } from "./TransactionList";

export function TransactionManager() {
  const [formOpen, setFormOpen] = useState(false);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-foreground text-xl font-semibold">Transações</h2>
          <p className="text-muted-foreground text-sm">
            Lançamentos únicos, recorrentes e transferências entre contas.
          </p>
        </div>
        <Button onClick={() => setFormOpen(true)}>
          <Plus className="h-4 w-4" />
          Nova transação
        </Button>
      </div>

      <TransactionList />

      <TransactionForm open={formOpen} onOpenChange={setFormOpen} />
    </div>
  );
}
