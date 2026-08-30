import { AccountManager } from "@/features/account/components/AccountManager";
import { TransactionManager } from "@/features/transactions/components/TransactionManager";
import { Separator } from "@/shared/components/ui/separator";

export default function AccountsPage() {
  return (
    <div className="flex flex-col gap-8">
      <AccountManager />
      <Separator />
      <TransactionManager />
    </div>
  );
}
