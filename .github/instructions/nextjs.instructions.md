---
applyTo: "**/*.{ts,tsx,js,jsx,config.*}"
---
# Next.js & Frontend Architecture Guidelines

## Project Context
- **Application:** Mithril-Vault (Personal Finance).
- **Stack:** Next.js (App Router), TypeScript, Tailwind CSS.
- **Backend:** Java Spring Boot (assume strict type matching with backend DTOs).

## Core Principles
1.  **Server Components First:** All components are React Server Components (RSC) by default. Only add `'use client'` when using hooks (`useState`, `useEffect`) or event listeners.
2.  **Nord Theme Enforcement:** Use the specific Nord color palette for all custom UI elements not covered by shadcn/ui.
3.  **Strict TypeScript:** No `any`. Define interfaces for all Props and API responses.

## Visual Design (Nord Theme)
The application uses the **Nord** color palette. Prefer these Tailwind classes or hex codes:
- **Polar Night (Dark Backgrounds):**
    - `#2E3440` (nord0) - Base background
    - `#3B4252` (nord1) - Lighter background / Panels
    - `#434C5E` (nord2) - Selection / Active line
    - `#4C566A` (nord3) - Comments / Ignored text
- **Snow Storm (Light Text/Elements):**
    - `#D8DEE9` (nord4) - Main text
    - `#E5E9F0` (nord5) - Brighter text
    - `#ECEFF4` (nord6) - Highlighted text / Light Mode Background
- **Frost (Accents/Primary):**
    - `#8FBCBB` (nord7) - Teals / Success-ish
    - `#88C0D0` (nord8) - Primary Blue
    - `#81A1C1` (nord9) - Secondary Blue
    - `#5E81AC` (nord10) - Dark Blue / Links
- **Aurora (States/Errors):**
    - `#BF616A` (nord11) - Red / Error
    - `#D08770` (nord12) - Orange / Warning
    - `#EBCB8B` (nord13) - Yellow
    - `#A3BE8C` (nord14) - Green / Success

## Coding Standards
- **Data Fetching:** Use `fetch` with `async/await` in Server Components. Avoid `axios` unless necessary for interceptors.
- **Project Structure:**
    - `app/`: Routes and pages.
    - `components/ui/`: Primitive shadcn components (do not modify manually).
    - `components/features/`: Complex, domain-specific components (e.g., `TransactionTable`, `BudgetChart`).
    - `lib/`: Utility functions and Zod schemas.
- **Forms:** Use `react-hook-form` combined with `zod` for validation.

## Interaction with Backend
- When fetching data from the Spring Boot backend, ensure the TypeScript interface exactly matches the Java Record/DTO structure.
- Handle loading states with `Suspense` and UI skeletons (`<Skeleton />`).