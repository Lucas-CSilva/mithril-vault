import Link from "next/link";

import {
  BarChart3,
  Bell,
  CreditCard,
  Download,
  LayoutDashboard,
  PiggyBank,
  RefreshCw,
  Target,
  Wallet,
} from "lucide-react";

import { LogoutButton } from "@/features/auth/components/LogoutButton";

const navItems = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/transactions", label: "Transactions", icon: BarChart3 },
  { href: "/accounts", label: "Accounts", icon: Wallet },
  { href: "/cards", label: "Cards", icon: CreditCard },
  { href: "/planning", label: "Planning", icon: Target },
  { href: "/investments", label: "Investments", icon: PiggyBank },
  { href: "/subscriptions", label: "Subscriptions", icon: RefreshCw },
  { href: "/import", label: "Import", icon: Download },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-background flex h-screen">
      <aside className="border-border bg-secondary flex w-64 flex-col border-r">
        <div className="flex h-16 items-center px-6">
          <span className="text-foreground text-lg font-semibold">
            Mithril Vault
          </span>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {navItems.map(({ href, label, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              className="text-foreground-2 hover:bg-input hover:text-foreground flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors"
            >
              <Icon size={18} />
              {label}
            </Link>
          ))}
        </nav>
      </aside>
      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="border-border bg-secondary flex h-16 items-center justify-end gap-2 border-b px-6">
          <button
            aria-label="Notificações"
            className="text-muted-foreground hover:bg-input rounded-full p-2 transition-colors"
          >
            <Bell size={20} />
          </button>
          <LogoutButton />
        </header>
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  );
}
