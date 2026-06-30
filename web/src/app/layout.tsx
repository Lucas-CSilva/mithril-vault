import { AuthProvider } from "@/core/contexts/AuthContext";
import QueryProvider from "@/shared/components/QueryProvider";

import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Mithril Vault",
  description:
    "A centralized dashboard for personal financial management, transaction tracking, and goal visualization.",
  keywords: [
    "finance",
    "budgeting",
    "expense tracking",
    "personal finance",
    "financial management",
  ],
  authors: [{ name: "Lucas Correia da Silva" }],
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#F5F6F6",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="bg-background text-foreground min-h-screen antialiased">
        <QueryProvider>
          <AuthProvider>{children}</AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
