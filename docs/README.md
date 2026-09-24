# Budget Manager

A simple personal app for managing a monthly budget. Set a budget per category at the start of the month, log what you spend (manually or from bank SMS), and see what is left in each category. Overspending is allowed and shown as a negative number.

- **Platform:** native Android app, single user
- **Data:** stored on the phone only
- **Phase:** Phase 1 (SMS, notification, and install testing) run and mostly confirmed working. Building the real app: M0-M4 done (project foundation, domain core and database, SMS receive/catch-up, Messages page, Add Transaction and the accept flow) - one M4 item (the Cancel button) has a fix shipped but not yet confirmed, see the "still open" list in `07-open-questions.md`. M5 (Categories) is next. See the progress tracker in `14-implementation-plan.md`.
- **Built with:** Kotlin and Jetpack Compose, in Android Studio
- **Code:** private repo at [BudgetManager-app](https://github.com/Prakhar-Swarnakar/BudgetManager-app)
- **Last updated:** 2026-09-25

## Documents

| File | What it covers |
|---|---|
| [01-product-overview.md](01-product-overview.md) | Vision, principles, and the log of decisions made so far |
| [02-features.md](02-features.md) | The v1 feature list |
| [03-user-flows.md](03-user-flows.md) | Step-by-step flows for each main journey |
| [04-messages-and-notifications.md](04-messages-and-notifications.md) | SMS capture, the Messages page, statuses, and the notification |
| [05-budget-rules.md](05-budget-rules.md) | How budgets, remaining amounts, alerts, and trend figures are calculated |
| [06-backlog.md](06-backlog.md) | Ideas deliberately postponed |
| [07-open-questions.md](07-open-questions.md) | Assumptions to confirm and questions still open |
| [08-pages-and-navigation.md](08-pages-and-navigation.md) | Navigation structure and what each page contains |
| [09-risks-and-phases.md](09-risks-and-phases.md) | What could go wrong, ranked by importance, and the four build phases |
| [10-phase-1-guide.md](10-phase-1-guide.md) | Step-by-step guide to build and test the Phase 1 app in Android Studio |
| [11-phase-1-results.md](11-phase-1-results.md) | Blank results sheet to fill in while testing |
| [phase1-test-app](phase1-test-app) | Kotlin source files for the Phase 1 test app |
| [12-tech-architecture.md](12-tech-architecture.md) | Technology choices, architecture, folder structure, reusable components, and data model for the real app |
| [13-development-best-practices.md](13-development-best-practices.md) | Coding, testing, data, privacy, and release rules |
| [14-implementation-plan.md](14-implementation-plan.md) | **Start here to build.** The milestone plan M0 to M12, setup, working method, and next steps |
| [sms-samples-template.txt](sms-samples-template.txt) | Template for collecting real bank SMS for the parser tests |

## UI mockups

Rendered screens are in the [ui-mockups](ui-mockups) folder. Start with `00-overview.png`, which shows every screen on one sheet. The other files are one screen each and are listed in [08-pages-and-navigation.md](08-pages-and-navigation.md).

## Status labels used in these docs

- **Decided:** you confirmed it.
- **Assumed:** a reasonable default I filled in. Please confirm or correct.
- **Backlog:** agreed to postpone; not in v1.
