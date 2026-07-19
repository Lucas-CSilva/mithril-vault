"use client";

import { RadioGroup, RadioGroupItem } from "@/shared/components/ui/radio-group";
import { cn } from "@/shared/utils";

import { CATEGORY_ICON_KEYS, CategoryIcon } from "../lib/icon-map";

interface IconPickerProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export function IconPicker({ value, onChange, disabled }: IconPickerProps) {
  return (
    <RadioGroup
      value={value ?? ""}
      onValueChange={onChange}
      disabled={disabled}
      className="flex flex-wrap gap-2"
    >
      {CATEGORY_ICON_KEYS.map((iconKey) => (
        <label
          key={iconKey}
          htmlFor={`category-icon-${iconKey}`}
          className={cn(
            "border-input text-muted-foreground flex h-9 w-9 items-center justify-center rounded-md border transition-colors",
            "has-[[data-state=checked]]:border-ring has-[[data-state=checked]]:text-foreground has-[[data-state=checked]]:bg-accent",
            disabled ? "cursor-not-allowed opacity-50" : "cursor-pointer",
          )}
        >
          <RadioGroupItem
            value={iconKey}
            id={`category-icon-${iconKey}`}
            className="sr-only"
          />
          <CategoryIcon icon={iconKey} className="h-4 w-4" />
          <span className="sr-only">{iconKey}</span>
        </label>
      ))}
    </RadioGroup>
  );
}
