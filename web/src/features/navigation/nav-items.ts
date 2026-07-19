import {
  CreditCard,
  LayoutGrid,
  PiggyBank,
  RefreshCw,
  Target,
  Wallet,
  type LucideIcon,
} from "lucide-react";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

export const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Visão Geral", icon: LayoutGrid },
  { href: "/accounts", label: "Contas", icon: Wallet },
  { href: "/cards", label: "Cartões", icon: CreditCard },
  { href: "/planning", label: "Planejamento", icon: Target },
  { href: "/investments", label: "Investimentos", icon: PiggyBank },
  { href: "/subscriptions", label: "Assinaturas", icon: RefreshCw },
];

export const PAGE_META: Record<string, { title: string; sub: string }> = {
  "/dashboard": {
    title: "Visão Geral",
    sub: "Sua saúde financeira em tempo real",
  },
  "/accounts": {
    title: "Contas & Transações",
    sub: "Todo movimento de dinheiro, em um só lugar",
  },
  "/cards": { title: "Cartões", sub: "Limites, faturas e ciclos de cobrança" },
  "/planning": {
    title: "Planejamento",
    sub: "Orçamentos e cofres de objetivos",
  },
  "/investments": { title: "Investimentos", sub: "Carteira de renda fixa" },
  "/subscriptions": {
    title: "Assinaturas",
    sub: "Sua economia recorrente sob controle",
  },
};
