"use client";

import { useState } from "react";

import { Eye, EyeOff, Lock } from "lucide-react";

import { AuthField } from "./AuthField";

type PasswordFieldProps = {
  label: string;
  error?: string;
} & Omit<React.ComponentPropsWithRef<"input">, "type">;

export function PasswordField({
  label,
  error,
  ref,
  ...inputProps
}: PasswordFieldProps) {
  const [show, setShow] = useState(false);

  return (
    <AuthField label={label} icon={<Lock size={18} />} error={error}>
      <input
        ref={ref}
        type={show ? "text" : "password"}
        className="text-foreground placeholder:text-foreground-dim min-w-0 flex-1 bg-transparent px-[14px] py-[13px] text-[15px] outline-none"
        {...inputProps}
      />
      <button
        type="button"
        tabIndex={-1}
        aria-label={show ? "Ocultar senha" : "Mostrar senha"}
        onClick={() => setShow((s) => !s)}
        className="text-foreground-dim hover:bg-background hover:text-foreground-2 mr-1 grid h-[40px] w-[40px] flex-shrink-0 place-items-center rounded-[9px] transition-colors"
      >
        {show ? <EyeOff size={17} /> : <Eye size={17} />}
      </button>
    </AuthField>
  );
}
