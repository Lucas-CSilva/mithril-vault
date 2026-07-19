import {
  ArrowDownLeft,
  ArrowLeftRight,
  BookOpen,
  Car,
  Fuel,
  Heart,
  Home,
  MoreHorizontal,
  Play,
  Repeat,
  Shirt,
  ShoppingBag,
  ShoppingCart,
  Sparkles,
  TrendingUp,
  Utensils,
  Zap,
  type LucideIcon,
} from "lucide-react";

export const CATEGORY_ICON_MAP: Record<string, LucideIcon> = {
  cart: ShoppingCart,
  home: Home,
  car: Car,
  heart: Heart,
  book: BookOpen,
  sparkle: Sparkles,
  shirt: Shirt,
  repeat: Repeat,
  trending: TrendingUp,
  swap: ArrowLeftRight,
  "arrow-down-left": ArrowDownLeft,
  dots: MoreHorizontal,
  utensils: Utensils,
  bag: ShoppingBag,
  bolt: Zap,
  fuel: Fuel,
  play: Play,
};

export const CATEGORY_ICON_KEYS = Object.keys(CATEGORY_ICON_MAP);

export const DEFAULT_CATEGORY_ICON: LucideIcon = MoreHorizontal;

export function getCategoryIcon(icon: string | null | undefined): LucideIcon {
  if (!icon) return DEFAULT_CATEGORY_ICON;
  return CATEGORY_ICON_MAP[icon] ?? DEFAULT_CATEGORY_ICON;
}

interface CategoryIconProps {
  icon?: string | null;
  className?: string;
}

export function CategoryIcon({ icon, className }: CategoryIconProps) {
  // getCategoryIcon looks up a stable component reference in a module-level
  // Record — it never creates a new component, so this isn't the unstable
  // dynamic-component pattern the rule guards against.
  const Icon = getCategoryIcon(icon);
  // eslint-disable-next-line react-hooks/static-components
  return <Icon className={className} />;
}
