---
name: shadcn-workflow
description: How to add and use shadcn/ui components in web/ via the shadcn MCP server. Read before building any UI feature with shadcn components.
---

# shadcn/ui Workflow

The shadcn MCP server (configured in the repo `.mcp.json`) is the authority for component code.
**Do not hallucinate component source — fetch it.** Components install into
`src/shared/components/ui` (alias `@/shared/components/ui`); `components.json` uses the
"new-york" style with the `neutral` base color.

## Workflow for any UI request

1. **Plan** — deconstruct the request into the components/blocks needed.
2. **Search** — `list_components` / `search_components` to map functionality to component names
   (e.g. "date picker" → `calendar`).
3. **Fetch demos (mandatory)** — `get_component_demo` / `get_component_examples` to get the
   correct, current usage before writing any code.
4. **Install** — `install-component`, or output the exact CLI in a bash block:
   `pnpm dlx shadcn@latest add <component>`. Never hand-copy implementation code.
5. **Generate** — write the React code based only on the fetched examples, importing from
   `@/shared/components/ui`.

## Tool guide

| Tool | Use for |
|---|---|
| `list_components` | "What's available?" / discovery |
| `search_components` | Map a need to a component name |
| `get_component_demo` | Source-of-truth usage — **before coding a component** |
| `install-component` | Generate/run the install command |
| `get_block` | Full sections (dashboards, auth forms) |

## Rules

- Maintain Nord theme + Tailwind tokens (see `web/CLAUDE.md`).
- Assets go in `/public/icons` and `/public/images`; avoid inline raw SVG unless necessary
  (Lucide is the icon library).
- Keep imports aligned with the `@/shared/components/ui` alias.
