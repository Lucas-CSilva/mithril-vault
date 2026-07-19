"use client";

import { Check } from "lucide-react";

import { RadioGroup, RadioGroupItem } from "@/shared/components/ui/radio-group";
import { cn } from "@/shared/utils";

import { CATEGORY_COLORS } from "../lib/colors";

interface ColorPickerProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export function ColorPicker({ value, onChange, disabled }: ColorPickerProps) {
  return (
    <RadioGroup
      value={value ?? ""}
      onValueChange={onChange}
      disabled={disabled}
      className="flex flex-wrap gap-2"
    >
      {CATEGORY_COLORS.map((color) => (
        <label
          key={color}
          htmlFor={`category-color-${color}`}
          className={cn(
            "relative flex h-8 w-8 items-center justify-center rounded-full ring-offset-2 transition-shadow",
            "has-[[data-state=checked]]:ring-ring has-[[data-state=checked]]:ring-2",
            disabled ? "cursor-not-allowed opacity-50" : "cursor-pointer",
          )}
          style={{ backgroundColor: color }}
        >
          <RadioGroupItem
            value={color}
            id={`category-color-${color}`}
            className="sr-only"
          />
          {value === color && <Check className="h-4 w-4 text-white" />}
          <span className="sr-only">{color}</span>
        </label>
      ))}
    </RadioGroup>
  );
}
