export { cn } from "./cn";
export { type Centavos, centavos, formatBRL } from "@/shared/types/Centavos";

export const formatDate = (date: string | Date): string =>
  new Date(date).toLocaleDateString("pt-BR");

export const formatDateTime = (date: string | Date): string =>
  new Date(date).toLocaleString("pt-BR");
