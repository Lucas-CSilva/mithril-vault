---
applyTo: "**/*.{tsx,jsx,json,ts}"
---
# Shadcn/UI & MCP Server Protocol

You are connected to a Shadcn/UI MCP Server. You act as an interface to this server.

## Core Principles
1.  **MCP Authority:** You act as a bridge to the MCP server tools (`list_components`, `install-component`, etc.). Do not hallucinate component code; fetch it.
2.  **Installation:** ALWAYS use `install-component` (or output the `npx shadcn@latest add` command) to add components. Never manually copy-paste implementation code unless verifying against the installed version.
3.  **Tailwind CSS:** Use Tailwind utility classes strictly. Maintain consistency with the `shadcn/ui` design tokens and the "Nord Theme".
4.  **Assets:** Use `/public/icons` and `/public/images`. Do not use raw SVGs inline unless necessary.

## Workflow for UI Requests
When I ask for a UI feature, follow this strict sequence:
1.  **Plan:** Deconstruct the request.
2.  **Search:** Use `list_components` if unsure what is available.
3.  **Fetch Demos:** MANDATORY. Use `get_component_demo` or `get_component_examples` to see the correct usage patterns.
4.  **Install:** Execute/Suggest `install-component` for dependencies.
5.  **Generate:** Write the React code based *only* on the fetched examples.

## Tool Usage Guide
- **`list_components`:** Use when I ask "What can I use?" or for discovery.
- **`get_component_demo`:** Use BEFORE coding any component to get the source of truth.
- **`install-component`:** Use to generate the installation CLI command.
- **`get_block`:** Use for full sections (dashboards, auth forms).
- **`search_components`:** Use to map functionality to component names (e.g., "date picker" -> `calendar`).

## Formatting Output
- When suggesting installation, provide the exact `npx` command in a bash code block.
- When writing component code, ensure imports match the `@/components/ui` alias configured in `tsconfig.json`.