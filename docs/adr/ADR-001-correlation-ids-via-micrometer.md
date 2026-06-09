# ADR-001: Correlation IDs propagate via Micrometer Observation / Tracing, not hand-rolled Reactor Context

> **Document metadata**
> - **ID:** ADR-001
> - **Status:** Accepted
> - **Author:** Architect
> - **Date:** 2026-06-08
> - **Linked spec:** `docs/architecture-contract.md` §P13 (Observability); `docs/product-definition.md` Part 4 (Number/Date formatting & cross-cutting concerns)
> - **Affects:** every feature's `ErrorResponse.correlationId` and `X-Correlation-Id` response header (`specs/001-auth` … `specs/011-subscriptions`)

---

## Context

Every API response carries a `correlationId` (returned in the `X-Correlation-Id` response header
and the `correlationId` field of `ErrorResponse`) so a client-reported error can be traced to its
server-side log lines. This is part of the API contract in all eleven feature OpenAPI specs and is
unchanged by this decision.

The open question is the *mechanism* by which the backend generates that id and makes it appear on
every log line for the request. The stack is reactive (Spring WebFlux / Project Reactor on Spring
Boot 4.0.6). The hard constraint is that **raw thread-local MDC silently breaks across reactor
thread hops** — a request can change threads between operators, and a value stuffed into MDC on the
request thread is not visible on the thread where the log call actually fires.

The architecture contract (P13) previously mandated propagating the id by hand through the Reactor
`Context`: a `WebFilter` writes a UUID into the context and handlers retrieve it with
`Mono.deferContextual`. That works, but it has costs:

- It is **manual plumbing in every place that logs** — `deferContextual` wrappers leak into
  handler code that otherwise has no reason to know about correlation ids.
- It is a **parallel, home-grown tracing context** that drifts from any real trace/span context.
  The moment we add Micrometer metrics or distributed tracing (already anticipated in P13:
  "Metrics via Micrometer (Prometheus) when introduced"), we would have two competing notions of
  request identity.
- It does **not** automatically bridge the Reactor `Context` back into MDC for the Logback JSON
  encoder, so we would *still* need a context-to-MDC bridge — i.e. we would be re-implementing a
  slice of what Micrometer's context-propagation library already does.

Spring Boot Actuator already auto-configures a server `Observation` per HTTP request. Micrometer
Tracing (with the `context-propagation` library and `Hooks.enableAutomaticContextPropagation()`)
restores that observation/trace context across reactor thread boundaries and exposes `traceId` /
`spanId` to MDC for the logging framework — exactly the cross-hop-safe behaviour we need, as a
supported, maintained capability rather than bespoke code.

---

## Decision

We will propagate request correlation IDs using **Micrometer Observation + Micrometer Tracing**,
not a hand-rolled Reactor `Context` / `Mono.deferContextual` scheme and not raw thread-local MDC.

Concretely:

- Each request is wrapped in the WebFlux server `Observation` auto-configured by Boot Actuator.
- We add a Micrometer Tracing bridge (`micrometer-tracing-bridge-otel` or `-brave`) plus the
  `io.micrometer:context-propagation` library, and call
  `Hooks.enableAutomaticContextPropagation()` once at startup.
- A thin `WebFilter` reads an inbound `X-Correlation-Id` header (or generates a UUID when absent),
  associates it with the current observation/trace, and writes it back on the response header. It
  does **not** push the id into the Reactor `Context` for handlers to fish out.
- Logback's JSON encoder includes the Micrometer-provided MDC keys (`traceId`/`spanId`), surfaced
  as `correlationId`, so every log line for the request carries it without per-call plumbing.

The **API contract is unchanged**: clients still receive `X-Correlation-Id` and
`ErrorResponse.correlationId`. Only the internal propagation mechanism changes.

---

## Status

`Accepted` — 2026-06-08

---

## Consequences

**Positive:**

- One observability context. Correlation, metrics (Prometheus, already planned in P13), and any
  future distributed tracing share the same Micrometer-rooted machinery instead of competing.
- No correlation-id plumbing leaks into handlers. `deferContextual` disappears from business code;
  the id is ambient via the observation/trace context and MDC.
- Cross-thread-hop safety is provided by a maintained library (`context-propagation` +
  `Hooks.enableAutomaticContextPropagation()`), not by code we have to keep correct ourselves.
- `traceId`/`spanId` in structured logs make logs greppable and align with industry tooling
  (OTel/Brave collectors) if we ever export traces.

**Negative:**

- Adds dependencies (`micrometer-tracing-bridge-*`, `context-propagation`, and a tracer such as
  OTel or Brave) beyond the Actuator+Micrometer-core already present.
- `Hooks.enableAutomaticContextPropagation()` is a global, process-wide switch; developers must
  understand that context now flows automatically (a behavioural change from "nothing flows unless
  you `deferContextual` it").
- Slightly more configuration surface (sampling, exporter) than a UUID-in-a-filter, even though we
  only need the correlation/MDC slice initially.

**Risks and mitigations:**

- *Risk:* sampling drops spans and a request ends up with no trace id. *Mitigation:* keep
  local/dev sampling at 100% and always fall back to the `WebFilter`-generated UUID so a
  `correlationId` is guaranteed regardless of trace sampling.
- *Risk:* automatic context propagation interacts badly with a custom operator. *Mitigation:* it is
  the Reactor-blessed mechanism for this exact purpose; covered by an integration test asserting the
  same `correlationId` appears in the response header and in log MDC across an async hop.

---

## Alternatives Considered

### Option A: Hand-rolled Reactor `Context` + `Mono.deferContextual` (rejected — was the prior rule)

A `WebFilter` writes a UUID into the Reactor `Context`; handlers/log sites retrieve it with
`Mono.deferContextual`.

**Rejected because:**

- Requires manual `deferContextual` plumbing wherever the id is needed, leaking a cross-cutting
  concern into business code.
- Builds a parallel request-identity context that diverges from Micrometer metrics/tracing we
  already plan to introduce.
- Still needs a separate Context→MDC bridge for the Logback JSON encoder — re-implementing part of
  what `context-propagation` provides.

### Option B: Raw thread-local MDC only (rejected)

Set the correlation id in MDC in a filter and read it from MDC at log time.

**Rejected because:**

- MDC is thread-local; reactor freely switches threads between operators, so the value is silently
  lost on the thread that actually emits the log line. This is the original failure mode P13 calls
  out.

### Option C: Full distributed-tracing backend (OTel Collector + Jaeger/Tempo) from day one (deferred)

Export spans to a tracing backend immediately.

**Deferred because:**

- For a single-service personal-finance app the immediate need is a correlatable id in structured
  logs, not a span UI. We adopt the Micrometer Tracing *bridge* (which gives us the id and MDC)
  now, and can attach an exporter/collector later with no code change — only configuration.

---

## References

- `docs/architecture-contract.md` §P13 — Observability (this ADR is cited there)
- `docs/implementation-plan.md` — Cross-Cutting Pre-Flight › Backend (observability wiring task)
- `api/CLAUDE.md` — Reactive rules (correlation IDs via Micrometer)
- Feature OpenAPI contracts `specs/001-auth … 011-subscriptions` — `ErrorResponse.correlationId`
  and `X-Correlation-Id` (contract surface, unchanged)
- Micrometer Tracing & `context-propagation`; Reactor `Hooks.enableAutomaticContextPropagation()`
