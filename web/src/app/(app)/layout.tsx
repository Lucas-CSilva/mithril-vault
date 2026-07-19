import { AppShell } from "@/features/navigation/components/AppShell";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
