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
    <div className="flex h-screen bg-[#ECEFF4]">
      <aside className="flex w-64 flex-col border-r border-[#D8DEE9] bg-[#E5E9F0]">
        <div className="flex h-16 items-center px-6">
          <span className="text-lg font-semibold text-[#2E3440]">
            Mithril Vault
          </span>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {navItems.map(({ href, label, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-[#3B4252] transition-colors hover:bg-[#D8DEE9] hover:text-[#2E3440]"
            >
              <Icon size={18} />
              {label}
            </Link>
          ))}
        </nav>
      </aside>
      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex h-16 items-center justify-end gap-2 border-b border-[#D8DEE9] bg-[#E5E9F0] px-6">
          <button
            aria-label="Notificações"
            className="rounded-full p-2 text-[#4C566A] transition-colors hover:bg-[#D8DEE9]"
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
