import { AlertCircle } from "lucide-react";

import { cn } from "@/shared/utils/cn";

interface AuthFieldProps {
  label: string;
  icon: React.ReactNode;
  error?: string;
  children: React.ReactNode;
}

export function AuthField({ label, icon, error, children }: AuthFieldProps) {
  return (
    <div className="flex flex-col gap-[7px]">
      <label className="text-foreground-2 text-[12.5px] font-semibold tracking-[-0.005em]">
        {label}
      </label>
      <div
        className={cn(
          "group bg-card relative flex items-center rounded-[14px] border transition-all duration-150",
          error
            ? "border-destructive focus-within:shadow-[0_0_0_3.5px_var(--error-bg)]"
            : "border-input hover:border-border-strong focus-within:border-ring focus-within:shadow-[0_0_0_3.5px_var(--ring-bg)]",
        )}
      >
        <span className="text-foreground-dim group-focus-within:text-ring ml-[14px] flex-shrink-0 transition-colors">
          {icon}
        </span>
        {children}
      </div>
      {error && (
        <p className="text-error-foreground flex items-center gap-[5px] text-[12px]">
          <AlertCircle size={13} className="flex-shrink-0" />
          {error}
        </p>
      )}
    </div>
  );
}
