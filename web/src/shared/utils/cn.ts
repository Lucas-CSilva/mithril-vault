import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Utility function to merge Tailwind CSS classes
 * Combines clsx for conditional classes with tailwind-merge for conflict resolution
 *
 * @param inputs - Class names or conditional class objects
 * @returns Merged class string with conflicts resolved
 *
 * @example
 * cn("px-4", "px-2") // Returns "px-2" (last one wins)
 * cn("px-4", isActive && "bg-primary") // Conditionally adds class
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
