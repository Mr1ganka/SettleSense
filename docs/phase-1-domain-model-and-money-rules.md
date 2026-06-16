# Phase 1: Domain Model And Money Rules

This document fixes the first version of the SettleSense domain model before deeper backend implementation. The goal is that the system can always explain a statement like "A owes B Rs 500" from source records.

## Core Principles

- Money is stored as integer minor units only. For INR, store paise.
- Every monetary amount has a currency code, such as `INR`.
- A group has one settlement currency in Phase 1. Expenses, splits, settlements, ledger entries, and balance projections inside that group must use that currency.
- Source records are not physically deleted after they affect money. They are cancelled or archived with audit metadata.
- Balances are derived, not hand-edited. The ledger is the explainable source for balances.
- Expense splits and settlement payments are append-only from a balance perspective. Edits create reversing ledger entries and replacement entries.

## Entities

### User

Represents a person who can participate in friendships, groups, expenses, and settlements.

Required fields:

- `id`
- `displayName`
- `email` or external auth subject
- `status`: `ACTIVE`, `DEACTIVATED`
- `createdAt`
- `updatedAt`

Rules:

- Deactivated users remain visible in historical group records.
- A user cannot be hard-deleted if referenced by expenses, settlements, or ledger entries.

### Friendship

Represents a relationship between two users outside a group.

Required fields:

- `id`
- `requesterUserId`
- `addresseeUserId`
- `status`: `PENDING`, `ACCEPTED`, `BLOCKED`, `CANCELLED`
- `createdAt`
- `updatedAt`

Rules:

- A friendship is unordered once accepted. The pair `(userA, userB)` must be unique regardless of direction.
- Friendships help with discovery and invitations; they do not create balances by themselves.

### Group

Represents a shared context such as a trip, flat, event, or recurring household.

Required fields:

- `id`
- `name`
- `currencyCode`
- `status`: `ACTIVE`, `ARCHIVED`
- `createdByUserId`
- `createdAt`
- `updatedAt`

Rules:

- `currencyCode` is immutable after the first expense or settlement is created.
- Archived groups keep all historical records and balance explanations.

### GroupMember

Represents a user's membership in a group.

Required fields:

- `id`
- `groupId`
- `userId`
- `role`: `OWNER`, `MEMBER`
- `status`: `ACTIVE`, `LEFT`, `REMOVED`
- `joinedAt`
- `leftAt`

Rules:

- A user can leave a group, but their historical expenses, splits, settlements, and balances remain.
- New expenses cannot include inactive members unless the expense date is before the membership ended and the actor has permission to backdate.

### Expense

Represents a bill or cost paid by one or more group members.

Required fields:

- `id`
- `groupId`
- `description`
- `currencyCode`
- `totalMinor`
- `expenseDate`
- `status`: `POSTED`, `CANCELLED`
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `cancelledAt`
- `cancelledByUserId`
- `cancellationReason`

Rules:

- `totalMinor` must be greater than zero.
- The expense currency must equal the group currency.
- The total paid amount must equal `totalMinor`.
- The total split amount must equal `totalMinor`.
- Cancelling an expense does not remove it. It adds reversing ledger entries.

Phase 1 supports one payer per expense in the primary model. Multi-payer support should be added as an `ExpensePayment` child entity later if needed. Until then, `paidByUserId` can live on `Expense`.

### ExpenseSplit

Represents how an expense is allocated to participants.

Required fields:

- `id`
- `expenseId`
- `owedByUserId`
- `splitType`: `EQUAL`, `EXACT`, `PERCENTAGE`, `SHARE`
- `inputValue`
- `amountMinor`
- `currencyCode`
- `createdAt`

Rules:

- Split rows store the final computed `amountMinor`, not only the user input.
- The sum of all split `amountMinor` values for an expense must equal the expense `totalMinor`.
- All split users must be group members.
- Split rows are immutable after the expense is posted. Editing an expense creates reversal and replacement records.

Rounding:

- Equal, percentage, and share splits may create a remainder because money is indivisible.
- Compute base amounts with integer division.
- Assign remaining minor units deterministically by split row order.
- The default order is ascending `owedByUserId`, unless the API explicitly supplies a stable participant order.
- Store final `amountMinor` on every split so future balance calculations do not depend on recalculating rounding.

### Settlement

Represents an actual payment from one user to another to reduce debt.

Required fields:

- `id`
- `groupId`
- `fromUserId`
- `toUserId`
- `currencyCode`
- `amountMinor`
- `settlementDate`
- `status`: `POSTED`, `CANCELLED`
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `cancelledAt`
- `cancelledByUserId`
- `cancellationReason`

Rules:

- `amountMinor` must be greater than zero.
- The settlement currency must equal the group currency.
- `fromUserId` and `toUserId` must be different.
- A settlement affects balances by recording that `fromUserId` paid `toUserId`.
- A settlement can overpay an existing debt. If A owed B Rs 500 and settles Rs 700, the balance becomes B owes A Rs 200.
- Cancelling a settlement adds reversing ledger entries.

### LedgerEntry

Represents a normalized money movement used to explain balances.

Required fields:

- `id`
- `groupId`
- `sourceType`: `EXPENSE`, `SETTLEMENT`, `REVERSAL`
- `sourceId`
- `fromUserId`
- `toUserId`
- `currencyCode`
- `amountMinor`
- `direction`: `OWES`, `PAID`
- `createdAt`

Rules:

- Ledger entries are append-only.
- `fromUserId` is the user whose balance moves toward owing or paying.
- `toUserId` is the counterparty.
- `amountMinor` must be greater than zero.
- Reversals point back to the original source through `sourceType` and `sourceId`.

Expense ledger rule:

- For each split where the payer and owed user are different, create one ledger entry:
  - `sourceType = EXPENSE`
  - `fromUserId = owedByUserId`
  - `toUserId = paidByUserId`
  - `amountMinor = split.amountMinor`
  - `direction = OWES`
- Do not create a ledger entry for the payer's own split.

Settlement ledger rule:

- For each posted settlement, create one ledger entry:
  - `sourceType = SETTLEMENT`
  - `fromUserId = fromUserId`
  - `toUserId = toUserId`
  - `amountMinor = settlement.amountMinor`
  - `direction = PAID`

Cancellation ledger rule:

- Cancelling a posted expense or settlement creates opposite-effect ledger entries with `sourceType = REVERSAL`.
- Original ledger entries remain unchanged.

### BalanceProjection

Represents a derived view of outstanding balances.

Required fields:

- `groupId`
- `fromUserId`
- `toUserId`
- `currencyCode`
- `amountMinor`
- `computedAt`

Rules:

- A projection row means `fromUserId` owes `toUserId` `amountMinor`.
- Projections can be stored for read performance, but they must be rebuildable from ledger entries.
- Projection rows should only store positive amounts.
- For any pair of users, store at most one direction after netting.

Pairwise netting:

1. Sum all `OWES` entries from A to B.
2. Subtract all `PAID` entries from A to B.
3. Also account for the opposite direction.
4. If the net is positive, A owes B.
5. If the net is negative, B owes A.
6. If the net is zero, no balance row is stored.

### ActivityEvent

Represents the human-readable audit trail.

Required fields:

- `id`
- `groupId`
- `actorUserId`
- `eventType`
- `entityType`
- `entityId`
- `message`
- `metadata`
- `createdAt`

Rules:

- Activity events are append-only.
- Every posted, edited, cancelled, or settled money action creates an activity event.
- Activity events support product history, but ledger entries remain the financial source of truth.

### InsightRequest

Represents a request for analysis or explanation, such as "why do I owe B Rs 500?"

Required fields:

- `id`
- `groupId`
- `requestedByUserId`
- `requestType`
- `prompt`
- `status`: `PENDING`, `COMPLETED`, `FAILED`
- `result`
- `createdAt`
- `completedAt`

Rules:

- Insight results must cite ledger entries, expenses, settlements, or activity events.
- Insights do not change balances.

## How Splits Are Stored

Expense splits are stored as finalized money rows.

For an expense of Rs 1,000 paid by A and split equally between A, B, and C:

- Expense stores `totalMinor = 100000`, `currencyCode = INR`, `paidByUserId = A`.
- ExpenseSplit rows store:
  - A owes `33334` paise
  - B owes `33333` paise
  - C owes `33333` paise

Ledger entries are created only for non-payer splits:

- B owes A `33333` paise
- C owes A `33333` paise

A's own share does not create debt because A already paid it.

## How Settlements Affect Balances

A settlement records payment against the ledger, not a mutation of old expenses.

If B owes A Rs 333.33 and B records a settlement of Rs 100 to A:

- Add settlement ledger entry: B paid A `10000` paise.
- Balance projection becomes B owes A `23333` paise.

The expense still explains the original debt. The settlement explains the reduction.

## How Debt Simplification Works

SettleSense should support two balance views:

- Pairwise balances: exact debts between each pair, directly explainable from ledger entries.
- Simplified settlements: suggested payments that minimize the number of transfers inside a group.

Pairwise balances are authoritative for explanation. Simplified settlements are recommendations.

Simplification algorithm:

1. Compute each user's net position in the group.
2. Users with positive net are creditors.
3. Users with negative net are debtors.
4. Match the largest debtor with the largest creditor.
5. Create a suggested payment for the smaller absolute amount.
6. Reduce both positions and repeat until all positions are zero.

Example:

- A is owed Rs 500.
- B owes Rs 300.
- C owes Rs 200.

Suggested settlements:

- B pays A Rs 300.
- C pays A Rs 200.

Important:

- Suggested settlements do not affect balances until posted as `Settlement` records.
- Posted settlements create ledger entries and become part of the permanent explanation.

## Audit And History Rules

- Posted financial records are immutable from the balance perspective.
- Editing an expense creates:
  - cancellation/reversal entries for the old posted values
  - a replacement expense or replacement version
  - new ledger entries
  - activity events tying the edit together
- Every money-affecting action stores:
  - actor
  - timestamp
  - source entity
  - before/after metadata where practical
- Ledger entries are the financial audit trail.
- Activity events are the product audit trail.

## Deletes And Cancellations

Use soft deletion or cancellation for all financial records.

Expense cancellation:

- Set expense status to `CANCELLED`.
- Store cancellation actor, time, and reason.
- Append reversal ledger entries.
- Rebuild or update balance projections.

Settlement cancellation:

- Set settlement status to `CANCELLED`.
- Store cancellation actor, time, and reason.
- Append reversal ledger entries.
- Rebuild or update balance projections.

User removal:

- Mark `GroupMember.status` as `LEFT` or `REMOVED`.
- Keep the user in historical records.
- Existing balances remain until settled or cancelled by valid financial actions.

Group archive:

- Mark group as `ARCHIVED`.
- Disable new expenses and settlements unless explicitly reopened.
- Keep all explanations available.

## Producing "A Owes B Rs 500"

The statement is produced from ledger entries and projections:

1. Read all posted ledger entries for the group.
2. Apply reversals.
3. Net entries between A and B.
4. If A has a positive net debt to B of `50000` paise, the projection is "A owes B Rs 500".
5. Explain the result by listing contributing expenses and settlements.

Example:

- Expense 1: B paid Rs 800, split equally between A and B.
  - A owes B Rs 400.
- Expense 2: B paid Rs 400, split equally between A and B.
  - A owes B Rs 200.
- Settlement: A paid B Rs 100.
  - A's debt to B is reduced by Rs 100.

Result:

- A owes B Rs 500.

Explanation:

- Rs 400 from Expense 1.
- Rs 200 from Expense 2.
- Minus Rs 100 settlement from A to B.

## Phase 1 Test Targets

The first backend tests should cover:

- Money is stored and calculated as integer minor units.
- Equal splits distribute remainders deterministically.
- Exact splits must sum to the expense total.
- Percentage splits must sum to 100 percent before rounding.
- Share splits must produce final amounts that sum to the expense total.
- Payer's own split does not create debt.
- Expense ledger entries produce pairwise balances.
- Settlements reduce balances.
- Over-settlement reverses the balance direction.
- Expense cancellation reverses ledger impact.
- Settlement cancellation reverses ledger impact.
- Balance projections can be rebuilt from ledger entries.
- Simplified settlement suggestions match user net positions without mutating balances.
