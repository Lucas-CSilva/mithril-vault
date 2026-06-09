export type Centavos = number & { readonly __brand: "Centavos" };

export const centavos = (n: number): Centavos => n as Centavos;

export const formatBRL = (value: Centavos): string =>
  (value / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
