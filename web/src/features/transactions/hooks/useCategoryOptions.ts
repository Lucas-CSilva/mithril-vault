import { useQuery } from "@tanstack/react-query";

import { listCategoryOptions } from "../api";
import { transactionKeys } from "../keys";

export function useCategoryOptions() {
  return useQuery({
    queryKey: transactionKeys.categoryOptions,
    queryFn: listCategoryOptions,
  });
}
