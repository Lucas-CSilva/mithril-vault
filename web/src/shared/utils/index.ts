import { type Centavos, centavos } from "@/shared/types/Centavos";

export { cn } from "./cn";
export { type Centavos, centavos, formatBRL } from "@/shared/types/Centavos";

export const formatDate = (date: string | Date): string =>
  new Date(date).toLocaleDateString("pt-BR");

export const formatDateTime = (date: string | Date): string =>
  new Date(date).toLocaleString("pt-BR");

export const centavosToReaisInput = (value: Centavos): string =>
  (value / 100).toFixed(2).replace(".", ",");

export const reaisInputToCentavos = (input: string): Centavos =>
  centavos(
    Math.round(
      parseFloat(input.trim().replace(/\./g, "").replace(",", ".")) * 100,
    ),
  );
