# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

One person: the owner, using the app on their own Pixel 7a in India. They open it in two ways: many quick glances during the day (often right after paying for something) and an occasional sit-down review of the month.

## Product Purpose

A private, single-user monthly budget. The owner sets a budget per category, spends are captured from bank SMS (reviewed and accepted or rejected) or added by hand, and the app shows how the month is going. Success is knowing, at a glance and without doing any maths, how much has gone out this month and where.

## Positioning

Built around the owner's own bank SMS stream on the phone, with every spend passing a human accept/reject step. Nothing leaves the device.

## Operating Context

- Spends arrive as bank/UPI SMS and land on Messages as Not assigned until reviewed.
- Some spends are shared with friends ("Owed to you"); only the owner's share counts toward a budget.
- Cash is tracked separately on the Wallet page (top-ups minus spends paid with Cash).
- Statements from banks and credit cards can be imported to check what SMS detection missed.

## Capabilities and Constraints

- Native Android: Kotlin, Jetpack Compose, Material 3, Room, Hilt. Currency is the Indian rupee with Indian digit grouping (₹1,25,000).
- Overspending is allowed and shown as a negative amount, never blocked.
- Home's first priority (confirmed 2026-11-20): how much has been spent this month, overall and per category. Second: spend that was avoidable. Third: cash on hand.
- Avoidable spend (decided 2026-11-21): a per-transaction checkbox on Add Transaction, reviewed later by editing the transaction. Home shows your share of this month's avoidable transactions.

## Brand Commitments

- Plain Material 3: no gimmicky charts, no game-like touches.
- Home keeps its month selector, shows every category (nothing hidden behind "show more"), and keeps the bottom-right floating + button for adding a transaction.

## Evidence on Hand

- Real usage data lives only on the phone and is never copied into the repo. Mockups use clearly synthetic amounts.
- Existing mockups: `docs/ui-mockups/00-overview.html` (current screens) and proposals `01`–`03`.

## Product Principles

1. Answer "how much has gone out?" before anything else.
2. Never make the owner do arithmetic the app can do.
3. Every number is traceable: a tap leads to the transactions behind it.
4. Private by construction: nothing leaves the phone.
5. Colour is never the only signal for a status.
