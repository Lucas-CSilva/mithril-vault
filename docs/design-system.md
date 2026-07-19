# Mithril Vault — Design System

> The single source of truth for how Mithril Vault looks, reads and behaves.
> Everything new — every screen, component and state — is built against this
> document. When code and this file disagree, this file wins; fix the code.

---

## 1. The direction — "Mithril"

**Quiet luxury, editorial, with a thin thread of fantasy.** A personal-finance
product that feels like a private bank's stationery: calm, silver-toned paper,
ink-navy type, hairline rules and serif numerals. The name made image — *a
precious metal: quiet, refined, indestructible.*

We explored three directions (Mithril · Tomo · Ágora). **Mithril is the one we
ship.** Tomo and Ágora are archived in `Design Directions v2.html` for reference
only — do not pull styles from them.

### Principles

1. **Hairlines over shadows.** Structure comes from 1px rules (`--line`), not
   heavy drop-shadows. Shadows are flat and barely-there.
2. **Air is a feature.** Generous whitespace, never dense dashboards. Let the
   numbers breathe.
3. **Serif for figures, sans for interface.** Money and headlines wear Spectral;
   labels, inputs and body wear Hanken Grotesk.
4. **One accent, used sparingly.** Ink-navy `--frost-deep` carries every primary
   action and active state. Resist adding colors.
5. **Honest numbers.** Tabular figures everywhere; the net balance already
   subtracts open invoices. No vanity metrics, no decorative stats.
6. **Light surface only.** No dark theme, no neon, no green-on-black "fintech"
   tropes. Subtly-toned whites and ink, nothing above ~0.02 chroma for paper.

---

## 2. Theming architecture

Themes are swapped live via a `[data-theme]` attribute on `<html>`. **Mithril is
the canonical, default theme** and the only one in active use. All design tokens
are CSS custom properties defined under `[data-theme="mithril"]` in
`app/styles.css`; components must consume tokens, never hard-coded hex.

```html
<html lang="pt-BR" data-theme="mithril">
```

> The codebase also carries legacy `cofre` / `pulse` / `vault` theme blocks from
> the exploration phase. These are **dead**. Build only against `mithril` tokens.

---

## 3. Color tokens

All values below are the live Mithril palette. **Always reference the token**,
not the hex.

### Surfaces & paper
| Token | Hex | Use |
|---|---|---|
| `--bg` | `#F5F6F6` | App background — silver paper |
| `--bg-tint` | `#EEEFF0` | Recessed background |
| `--surface` | `#FFFFFF` | Cards, inputs, raised surfaces |
| `--surface-2` | `#F4F5F6` | Subtle fills, hover backgrounds |
| `--surface-3` | `#EBECEE` | Tracks, inactive segments |

### Ink (text)
| Token | Hex | Use |
|---|---|---|
| `--ink` | `#1B2230` | Primary text, headlines, figures |
| `--ink-2` | `#474E5C` | Secondary text, field labels |
| `--ink-3` | `#767D89` | Tertiary / supporting copy |
| `--ink-4` | `#A2A7B0` | Muted, placeholders, eyebrows |

### Hairlines
| Token | Hex | Use |
|---|---|---|
| `--line` | `#E6E7EA` | Default border / divider |
| `--line-2` | `#DBDDE1` | Input borders, stronger dividers |
| `--line-strong` | `#C9CCD2` | Hover borders, checkbox outline, scrollbar |

### Accent — ink-navy / pewter ("frost")
| Token | Hex | Use |
|---|---|---|
| `--frost-deep` / `--accent` | `#3C5070` | **Primary** — buttons, active nav, links, focus |
| `--frost` | `#46597A` | Secondary accent, gradient mid-stop |
| `--frost-soft` | `#6B7C99` | Soft accent, muted marks |
| `--frost-teal` | `#5E7A96` | Chart / category variation |
| `--frost-pine` | `#8A93A3` | Chart / category variation |
| `--accent-bg` | `#EAEDF2` | Tinted accent fill, focus ring, selection |
| `--accent-line` | `#D6DBE3` | Accent-tinted border |

### Semantic — money & status
| Token | Hex | Use |
|---|---|---|
| `--pos` / `--pos-ink` | `#4E7C66` / `#3C6552` | Positive / credit (use the muted sage, never a bright green) |
| `--pos-bg` | `#E7F0EA` | Positive pill background |
| `--neg` / `--neg-ink` | `#8E3A4B` / `#7C2F3E` | Negative / debit / error (claret, not red) |
| `--neg-bg` | `#F4E9EB` | Negative pill / error-field background |
| `--warn` / `--warn-bg` | `#9E7A4E` / `#F2ECE0` | Warning, near-limit budgets |
| `--gold` | `#9E7A4E` | Accent figure (e.g. strength "good") |
| `--plum` | `#7E6A86` | Category accent only |

Helpers: `.money-pos` → `--pos-ink`, `.money-neg` → `--neg-ink`.

> **Money color discipline:** debits and credits use the muted claret/sage
> above — they are status hues, not alarms. Most of the ledger stays `--ink`.

---

## 4. Typography

Three families, loaded once in `styles.css`:

| Role | Token | Family | Used for |
|---|---|---|---|
| Display | `--display` | **Spectral** (serif) | `h1`/`h2`, money figures, error codes, brand wordmark |
| Sans | `--sans` | **Hanken Grotesk** | Everything UI — labels, inputs, body, buttons |
| Mono | `--mono` | **Spline Sans Mono** | Eyebrows, reference codes, meta, tabular accents |

### Rules
- **Headlines & figures → Spectral**, weight 500–600, tight tracking
  (`letter-spacing: -.01em` to `-.02em`).
- **Body & interface → Hanken Grotesk**, 15px base, line-height ~1.45.
- **Eyebrows** (`.eyebrow`): mono, 10.5px, `600`, `letter-spacing: .14–.20em`,
  uppercase, `--ink-4`. The signature small-label of the system.
- **All numbers are tabular.** `body` sets `font-feature-settings: 'tnum' 1`;
  `.num` / `.mono` re-assert it. Never let figures shift width.
- **Money cents are de-emphasised.** Render via `MV.fmtBRLParts()` and set the
  decimal (`,90`) smaller than the integer.

---

## 5. Numbers, money & locale

- **Locale is `pt-BR`. Currency is BRL (R$) only.** Set `lang="pt-BR"`.
- **Money is stored as centavos (integer)** and formatted only at render. Never
  store floats.
- Formatters live in `app/format.js` (`window.MV`):
  - `MV.fmtBRL(centavos, {cents})` → `R$ 12.480,90`
  - `MV.fmtBRLParts(centavos)` → `{ sign, symbol, int, dec }` for styled cents
  - `MV.fmtNum`, `MV.fmtPct`, `MV.fmtSignedPct`
  - `MV.fmtDate` (`dd/mm/aaaa`), `MV.fmtDateShort` (`06 jun`), `MV.fmtDayMonth`,
    `MV.relDays`, `MV.MESES`, `MV.MESES_FULL`
- Negative sign is the typographic minus `−` (U+2212), not a hyphen.
- Dates and month names are Portuguese and lowercase in compact contexts
  (`jun`, `abr`).

---

## 6. Shape, spacing & elevation

### Radii (Mithril overrides — refined corners)
`--r-xs: 5px` · `--r-sm: 8px` · `--r-md: 11px` · `--r-lg: 14px` · `--r-xl: 18px`

- Cards → `--r-lg`. Inputs / buttons → `--r-md`. Pills / tracks → `99px`.

### Shadows (airy, flat — lean on hairlines)
`--sh-sm` … `--sh-xl`, all low-alpha ink (`rgba(27,34,48,…)`). Use `--sh-sm` for
resting cards, `--sh-md` for raised glyphs, `--sh-lg/xl` only for true overlays.

### Layout
- Sidebar width: `--sidebar-w: 256px`.
- Responsive grids collapse to single column at `≤1100px`; sidebar hides at
  `≤880px`; auth brand panel hides at `≤940px`.

---

## 7. Iconography

- **Feather-style line icons**, defined as SVG paths in `app/icons.jsx` and drawn
  by `<Icon name size stroke />`.
- `viewBox 0 0 24 24`, `fill: none`, `stroke: currentColor`, `stroke-width: 2`
  (heavier `2.6–3` only for tiny checks), round caps & joins.
- Color icons by setting text color on the parent; never bake a hex into a path.
- Need a new glyph? Add a path to `ICONS` in the same style. **Do not** inline
  one-off SVGs in components, and never hand-draw illustrative SVGs — use a
  placeholder and ask for real assets.
- **No emoji** in the Mithril UI. (The Ágora exploration used emoji; that is not
  our system.)

---

## 8. Core components

### Brand mark — the ring-seal
The logo is an engraved "M" inside a double concentric ring (a seal). Two forms:
- `LogoLight` / `.brand-seal` — white ring on the dark brand panel.
- `LogoMark` / `.mark-seal` — accent ring on light backgrounds (errors, app).

The ring motif repeats as oversized concentric circles (`.brand-ring`,
`.error-watermark`) for quiet, on-brand decoration. **Never use diamonds or
neon.**

### Card — `.card`
`--surface` + `1px var(--line)` + `--r-lg` + `--sh-sm`. The default container.

### Pill — `.pill`
Mono, 11px, `600`, `99px` radius. Status chips pair a semantic `*-bg` with the
matching `*-ink`.

### Eyebrow — `.eyebrow`
The mono uppercase micro-label that opens most sections and cards.

### Buttons
- **Primary** (`.btn-primary` / `.auth-btn`): `--frost-deep` fill, white text,
  `--r-md`, `--sh-sm`; hover `brightness(1.06)`, active nudges down 1px.
- **Ghost** (`.btn-ghost` / `.ghost-link`): `--surface` + `--line-2` border,
  `--ink-2` text; hover lifts background and border.

### Form fields
- `.auth-field` → label (`--ink-2`, 12.5px, 600) + `.auth-input` shell.
- Shell: `--surface`, `1px --line-2`, `--r-md`; **focus →** `--frost-deep`
  border + `0 0 0 3.5px var(--accent-bg)` ring; **error →** `--neg` border +
  `--neg-bg` ring.
- Leading `--ink-4` icon that turns `--frost-deep` on focus. Password fields get
  an eye toggle; passwords on register show the 4-segment **strength meter**.
- Inline errors: `--neg-ink`, 12px, with `alertCircle` icon. Page-level errors
  use `.auth-banner` (`--neg-bg`).

### Auth layout
Split screen: dark ink-navy **brand panel** (gradient + dot lattice + rings +
feature list) on the left, scrollable **form panel** centered (max-width 388px)
on the right. Collapses to form-only `≤940px`.

### Error template
Centered card on a faintly accent-washed background with concentric ring
watermark: ring-seal brand → glyph → big Spectral **code** (middle digit in
`--frost-deep`) → eyebrow → title → message → primary + ghost actions → mono
reference code. Configured by the `ERRORS` map (`404/500/403`) in `auth.jsx`.

---

## 9. Motion

- Entrance: `.fade-in` — 6px rise + fade, `cubic-bezier(.22,.61,.36,1)`, ~0.45s.
- Theme/surface transitions ~0.35s ease.
- Spinners for pending submits (`.spin`).
- **Always honor `prefers-reduced-motion: reduce`** — the global rule disables
  all animation/transition. No infinite decorative loops.

---

## 10. UX & content rules

- **Voice:** Portuguese (pt-BR), calm, plain, second person ("Entre para
  continuar"). Confident, never hypey.
- **Data isolation is a promise.** Each user sees only their own records; it is
  surfaced in copy (auth foot, `403` page). Keep that language consistent.
- **Validation is gentle and specific.** Validate on submit (and clear a field's
  error as the user fixes it); messages say what to do, not just what's wrong.
- **Empty/error states stay on-brand:** ring-seal, calm message, one clear
  primary action plus a ghost "back".
- **Accessibility:** label every input (`htmlFor`/`id`), keep focus rings
  visible (`:focus-within` / `:focus-visible`), maintain AA contrast (ink-navy on
  white passes), hit targets ≥44px (buttons are 44–48px tall).

---

## 11. File map

| File | Role |
|---|---|
| `app/styles.css` | Tokens + base + reusable atoms (`.card`, `.pill`, `.eyebrow`) |
| `app/auth.css` | Auth split-screen + error template styles |
| `app/auth.jsx` | Login / Register / Error components + `mountAuth` |
| `app/icons.jsx` | `<Icon>` + the `ICONS` path set |
| `app/format.js` | `MV` money / number / date formatters |
| `Login.html`, `Register.html`, `Error 4xx/5xx.html` | Page entry points |
| `Mithril Vault.html` | Main app shell (dashboard, accounts, cards, …) |

**Golden rules:** consume tokens, not hex · Spectral for figures, Hanken for UI ·
hairlines over shadows · one accent · centavos in, BRL out · `pt-BR` always.
