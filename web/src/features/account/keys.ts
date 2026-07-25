export const accountKeys = {
  all: ["accounts"] as const,
  list: (includeInactive: boolean) =>
    ["accounts", "list", includeInactive] as const,
  detail: (id: string) => ["accounts", id] as const,
  balanceHistory: (id: string) => ["accounts", id, "balance-history"] as const,
};
