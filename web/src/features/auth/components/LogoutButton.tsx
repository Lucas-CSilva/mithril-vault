"use client";

import { LogOut } from "lucide-react";

import { useAuth } from "@/core/contexts/AuthContext";

export function LogoutButton() {
  const { logout } = useAuth();

  return (
    <button
      onClick={logout}
      aria-label="Sair"
      className="flex items-center gap-[7px] rounded-lg px-3 py-2 text-sm text-[#4C566A] transition-colors hover:bg-[#D8DEE9] hover:text-[#2E3440]"
    >
      <LogOut size={16} />
      Sair
    </button>
  );
}
