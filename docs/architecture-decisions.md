# SettleSense Architecture Decisions

This document records important architectural and technical decisions made during the development of SettleSense.

For the primary master document, see **[`docs/DECISIONS.md`](file:///C:/Users/kelvin/Documents/SettleSense/docs/DECISIONS.md)**.

---

## Decision Summary

| ADR ID | Decision Title | Status | Date |
| :--- | :--- | :--- | :--- |
| **ADR-001** | Layered Backend Architecture | Accepted | 2026-06-18 |
| **ADR-002** | PostgreSQL as Primary Database | Accepted | 2026-06-18 |
| **ADR-003** | Store Money as Integer Minor Units (Paise) | Accepted | 2026-06-18 |
| **ADR-004** | Ledger-Based Balance Calculation & Projections | Accepted | 2026-06-18 |
| **ADR-005** | Flyway Database Schema Evolution | Accepted | 2026-06-18 |
| **ADR-006** | Greedy Debt Simplification Algorithm | Accepted | 2026-06-19 |
| **ADR-007** | Reversing Ledger Entries over Hard Deletes | Accepted | 2026-06-20 |
| **ADR-008** | React + TypeScript + Vite Frontend Architecture | Accepted | 2026-06-21 |
| **ADR-009** | Redis-Backed Token Bucket Rate Limiting | Accepted | 2026-06-22 |

---

## Key Decision Highlights

### ADR-001: Layered Backend Architecture
Use a layered Spring Boot architecture (`Controller` → `Service` → `Repository` → `PostgreSQL`). Separates HTTP concern, business validation/split rules, database queries, and entity persistence.

### ADR-003: Store Money as Integer Minor Units
Store money strictly as integer minor units (`Long` in paise for INR). No floating-point (`double`/`float`) types in financial paths. Prevents rounding errors.

### ADR-004: Ledger-Based Balance Calculation
Calculate net balances from an immutable double-entry `LedgerEntry` system updated on every expense or settlement, caching projections in `BalanceProjection`.

### ADR-006: Greedy Debt Simplification Algorithm
Calculate net balances for all members, separate debtors and creditors, and greedily match largest debtor to largest creditor to reduce $O(N^2)$ direct debts down to at most $N-1$ settlement transfers.

### ADR-007: Reversing Ledger Entries over Hard Deletes
Financial entries are append-only. Modifying or cancelling expenses creates compensating reversing ledger entries to ensure full auditability.

See **[`docs/DECISIONS.md`](file:///C:/Users/kelvin/Documents/SettleSense/docs/DECISIONS.md)** for full decision records.
