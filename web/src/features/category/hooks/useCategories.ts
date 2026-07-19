import { useQuery } from "@tanstack/react-query";

import { listCategories } from "../api";
import { categoryKeys } from "../keys";

export function useCategories() {
  return useQuery({
    queryKey: categoryKeys.all,
    queryFn: listCategories,
  });
}
