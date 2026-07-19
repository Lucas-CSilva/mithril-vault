"use client";

import { LogOut } from "lucide-react";

import { useAuth } from "@/core/contexts/AuthContext";

export function LogoutButton() {
  const { logout } = useAuth();

  return (
    <button
      onClick={logout}
      aria-label="Sair"
      className="text-muted-foreground hover:bg-input hover:text-foreground flex items-center gap-[7px] rounded-lg px-3 py-2 text-sm transition-colors"
    >
      <LogOut size={16} />
      Sair
    </button>
  );
}
