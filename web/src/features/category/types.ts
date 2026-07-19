export interface Category {
  id: string;
  name: string;
  parentId: string | null;
  icon: string | null;
  color: string | null;
  isSystem: boolean;
}

export interface CreateCategoryCommand {
  name: string;
  parentId?: string;
  icon?: string;
  color?: string;
}

export interface UpdateCategoryCommand {
  name?: string;
  icon?: string;
  color?: string;
}
