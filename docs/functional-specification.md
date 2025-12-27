# Mithril-Vault: Product Functional Specification

> **Version:** 1.0
> **Status:** Product Definition
> **Visual Theme:** Nord Theme (Light Mode / Snow Storm)

## 1. Product Vision
Mithril-Vault is designed to serve as the user's definitive financial source of truth. It transforms raw transactional data into a coherent financial narrative, enabling users to understand their historical data, control current activities, and plan for future objectives. The system operates as a comprehensive financial management ecosystem, prioritizing clarity, security, and a holistic view of the user's net worth.

---

## 2. Design & UX Guidelines
* **Color Palette:** Strict adherence to the **Nord Theme**.
    * **Backgrounds:** Variations of *Snow Storm* (light/white).
    * **Primary Actions/Positive Data:** Shades of *Frost* (blues and cyans).
    * **Semantics/Alerts:** Shades of *Aurora* (reds, oranges, greens).
* **Layout:** High contrast for readability and generous use of whitespace to reduce cognitive load. The reference images serve solely as structural wireframes.

---

## 3. Module Details

### Module 1: Dashboard & Overview
The entry point of the system. Its primary objective is to assess the user's financial health immediately.

* **Unified Global Balance (Hero Section):**
    * Aggregation of total available funds (Checking Accounts + Cash on Hand).
    * Intentional exclusion of long-term investments to provide a realistic view of immediate liquidity.
* **Performance Summary (KPIs):**
    * **Inflow:** Total revenue collected in the current period.
    * **Outflow:** Total expenses incurred and committed.
    * **Savings/Reserves:** Net value saved or invested during the period.
* **Cash Flow Analytics:**
    * Interactive spline line chart comparing balance evolution (Weekly/Monthly/Yearly).
* **Expense Distribution:**
    * Donut Chart categorizing expenses (e.g., Housing, Leisure, Transport) with detailed views upon interaction.
* **Upcoming Obligations Radar:**
    * A compact, urgency-focused list: Bills, Credit Card Invoices, and Instant Payments (PIX) scheduled for the next 7 days.

### Module 2: Account & Transaction Management
The core operational component, focusing on granularity and accurate classification of movements.

* **Intelligent Activity Feed:**
    * Chronological list featuring semantic categorization.
    * **Method Tags:** Visual identification for `Debit`, `Credit`, `PIX`, `Bank Slip`, or `Recurring`.
* **Multi-Account Support:**
    * Registration of multiple sources (Physical and Digital Banks).
    * Statement Filtering: Unified View or Per Institution.
* **Reconciliation:**
    * Tools for manual adjustment of categories.
    * Input mechanism for cash transactions.

### Module 3: Card Management
Focuses on limit management and billing cycles. **Privacy Note:** The system does not process sensitive direct transaction data (PAN/CVV).

* **Card Wallet (Metadata):**
    * Visual representation (CSS Mockup) displaying only the last 4 digits.
    * Identification of Issuing Bank and Type (Physical/Virtual).
* **Invoice Cycle Monitor:**
    * Visual progress bar: Total Limit vs. Utilized vs. Available.
    * **Key Feature:** Highlighting the "Best Purchase Date" and "Invoice Closing Date".

### Module 4: Planning (Budget & Goals)
Proactive financial control tools.

* **Goal Vaults:**
    * Creation of specific objectives (e.g., "Travel Fund", "Emergency Reserve").
    * Tracking: Target Value vs. Current Value + Completion Projection.
* **Categorized Budgets:**
    * Definition of spending caps per category (e.g., Dining Out limit).
    * Visual progress alerts (Warning colors upon reaching 80%/100% of the cap).

### Module 5: Investment Hub
Consolidated view of assets beyond the checking account.

* **Aggregated Portfolio:**
    * Segmentation by asset class: Fixed Income, Equities, REITs, Crypto.
* **Net Worth Evolution:**
    * Area chart displaying the growth of Net Worth over time.
* **Position Detailing:**
    * Monitoring of Average Price, Current Value, and Variance (Profit/Loss).

### Module 6: Subscription Management
Control over recurring economy expenditures.

* **Renewal Calendar:**
    * Visual timeline of future charges (SaaS, Streaming, Utilities).
* **Total Cost Analysis:**
    * Clear visualization of the annual impact of subscriptions on the budget.
* **Automatic Categorization:**
    * Suggestions to convert recurring transactions into "Monitored Subscriptions".
