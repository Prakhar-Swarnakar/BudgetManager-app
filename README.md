# Budget Manager

A private, single-user Android budgeting app. Reads bank SMS to detect spends, lets you review and accept or reject each one, tracks a budget per category per month (overspending shown as a negative number), and charts trends over time. All data stays on the phone.

- **Platform:** native Android, Kotlin + Jetpack Compose, MVVM, Hilt, Room, DataStore
- **Status:** M0 to M6 done (project foundation, domain core and database, SMS receive/catch-up, Messages page, Add Transaction and the accept flow, Monthly budget). There is no separate Categories page - category management (create, rename, reorder) lives on Monthly budget, and archiving was dropped from v1. M7 (Home and Category detail) built, not yet tested on-device. M8 through M11 done, confirmed on-device (Alerts and Settings; Backup and restore; Trends - This month, Previous month, Historic, and the range setting). A post-v1 round (2026-10-01, from backlog planning) added a real SMS-dedupe fix, a possible-duplicate warning on Messages, category-suggestion learning from your choices, and an editable keyword-rules page under Settings - built, pending your own on-device test pass (see the test requirements shared separately) rather than self-verified this time. M12 (Hardening and release) is next. See the progress tracker in [docs/14-implementation-plan.md](docs/14-implementation-plan.md).
- **Docs:** product design, architecture, and the milestone plan live in [docs/](docs) - start at [docs/README.md](docs/README.md). Merged into this repository on 2026-09-25 (previously a separate repo), keeping its own commit history.

## Building

Open this folder in Android Studio and run on a device. See [docs/13-development-best-practices.md](docs/13-development-best-practices.md) for coding rules and [docs/14-implementation-plan.md](docs/14-implementation-plan.md) for the milestone plan and testing approach.
