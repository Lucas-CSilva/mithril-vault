"use client";

import { useState } from "react";

import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

export function AppShell({ children }: { children: React.ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="bg-background flex h-screen overflow-hidden">
      <div className="hidden lg:block">
        <Sidebar />
      </div>

      {mobileOpen && (
        <div
          className="fixed inset-0 z-[60] backdrop-blur-[2px] lg:hidden"
          style={{ background: "rgba(27,34,48,0.4)" }}
          onClick={() => setMobileOpen(false)}
        >
          <div
            className="h-full w-[var(--sidebar-w)]"
            onClick={(e) => e.stopPropagation()}
          >
            <Sidebar onNavigate={() => setMobileOpen(false)} />
          </div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Topbar onMenuClick={() => setMobileOpen(true)} />
        <main className="flex-1 overflow-x-hidden overflow-y-auto">
          <div className="mx-auto max-w-[1320px] px-[30px] pt-[26px] pb-[60px]">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
