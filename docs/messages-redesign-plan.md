# Messages redesign: implementation plan

Mockup: `docs/ui-mockups/05-messages-redesign.html` (published copy: https://claude.ai/artifact/7Kgf7qrXwCZjRAiMjpbhrh).

No database schema change, so no migration rehearsal. Internal enum names (`NOT_ASSIGNED`, `ACCEPTED`) stay; only on-screen words change.

Every phase follows the usual order: build, unit tests, then check on the phone. A phase is not done until the phone check passes. Each phase can ship on its own.

Risk key: **Low** = cosmetic or covered by existing tests. **Medium** = changes behaviour that has tests. **High** = touches money data or needs real-device behaviour.

## Phase 0: Renames and default tab

Risk: **Low**

- Labels become To review / Confirmed / Rejected / All, in that order. Reorder `MessageFilter` so the chips follow it.
- Replace "Not assigned" and "Accepted" in `MessagesContent`, `MessageRow`, `StatusBadge`, `MessageDetailSheet`, snackbar and empty-state text.
- Initial filter is To review. When it is empty, show the "All caught up" card.
- Home's "N messages to review" line already uses the right word.

Tests
- Rename the `MessagesViewModelTest` cases that say Accepted / Not assigned.
- New: the view model starts on `NOT_ASSIGNED`.
- New: a month with zero To review messages shows the caught-up state.

Phone check: open Messages, you land on To review; every label reads correctly.

## Phase 1: Row redesign and day groups

Risk: **Low to Medium** (the Avoidable query is new, the rest is layout)

- Avatar: category emoji (or first letter) in a circle, with a green check or red cross badge at the bottom right for Confirmed and Rejected. Built in `MessageRow` through `ActivityRow`'s existing leading slot, so Owed to you is untouched.
- Make `ActivityRow`'s status badge optional; drop the green and red row backgrounds.
- Amounts: "−₹486" and "+₹12,000" (green). Split spends show your share with the full amount struck through under it, from the existing `amountText` and `splitTotalText`. Messages with no amount show "No amount".
- Meta line: suggested category, "Split with N friends", or an amber "Avoidable" label.
- Avoidable needs one read-only query returning the flag per message, like `observeTitleBySourceMessage`. Add it to the DAO, both repositories (Room and Fake), and the interface.
- Day groups: sticky headers "Today", "Yesterday", then "Thu 19 Nov". Days with no messages are skipped. Right side: count left (To review) or that day's spend, your share only (Confirmed, All).

Tests
- Day grouping: today, yesterday, an older date, a month with gaps, a past month with no Today.
- Day spend counts your share only for split transactions.
- Avoidable flag reaches the row for a confirmed message and is absent for others.
- Existing amount, split and no-amount row tests still pass with the new sign format.

Phone check: confirmed split spend shows share plus struck full amount; Avoidable label matches the flag set on Add Transaction; headers stick while scrolling; dark mode and 1.3x font scale.

## Phase 2: Duplicate detection across statuses

Risk: **Medium** (changes a domain rule that has tests)

- `DetectPossibleDuplicates` currently ignores anything that is not To review. Change it to compare messages of every status. Keep the amount, different sender and 10 minute rules.
- `MessagesViewModel` passes the other message's status to the screen. Show the warning while neither message is rejected, and a quiet "matching message was rejected" note when one is.
- Guard: if the matching message is already confirmed, swipe-Confirm opens the detail sheet instead of going straight to Add Transaction.
- Row line: "Possible duplicate of Paytm, 12:06 pm, Confirmed".

Tests
- Update `DetectPossibleDuplicatesTest`: the test that says a confirmed message is never flagged is reversed. Keep the others.
- New: all four pairings (both to review, other confirmed, other rejected, both confirmed).
- New: the guard sends swipe-Confirm to the sheet, not to Add Transaction, when the other message is confirmed.
- New: a pair outside the 10 minute window or from the same sender is still not flagged.

Phone check: use a real or seeded pair; confirm one, and the other now shows the "already confirmed" wording.

## Phase 3: Undo on confirmed money out asks first

Risk: **High** (the action deletes a transaction and its friend splits)

- Today, `onSwipeEnd` on a confirmed debit calls `deleteBySourceMessage`. The database cascade also deletes its friend splits. Nothing brings them back.
- Add a confirm dialog (reuse `ConfirmDialog`) that names what goes: "Deletes 'Dinner at Toit' and its split with 2 friends. The message goes back to To review."
- Money in and rejected messages still undo straight away.
- Deliberately not built: a Restore snackbar. It would need a new method to put the transaction and its split rows back, plus tests. Revisit if the dialog proves annoying.

Tests
- Undo on a confirmed debit shows the dialog and deletes nothing until confirmed.
- Cancel leaves the transaction, splits and message status unchanged.
- Confirm deletes the transaction and returns the message to To review.
- Undo on confirmed money in and on rejected messages shows no dialog.

Phone check: confirm a split spend, undo it, cancel (nothing changes), undo again and confirm (Owed to you balance drops by the friend amounts). Check Home totals update.

## Phase 4: Swipe opens a button

Risk: **High** (custom gesture inside a scrolling list; only a real device settles it)

- New `RevealSwipeRow` in `feature/messages/components`. Messages only; Owed to you and Category detail keep today's `SwipeRow`.
- The row slides about 96 dp and shows one button, closing after 5 seconds, on tap elsewhere, on scroll, or when another row opens.
- Button actions by status:

| Status | Swipe right | Swipe left |
|---|---|---|
| To review | Confirm (green) | Reject (red) |
| Confirmed | nothing | Undo (grey) |
| Rejected | Undo (grey) | nothing |

- The button calls the existing `onSwipeStart` / `onSwipeEnd`, so view model rules do not change.
- TalkBack: each row gets the same actions as accessibility actions, so nothing depends on swiping.
- Reject keeps the Undo snackbar.

Tests
- View model tests stay as they are (the logic did not move).
- New: which row is open lives in screen state; opening a second row closes the first.
- New: the right button shows for each status and direction; the opposite direction shows nothing.

Phone check (the main reason this phase is isolated)
- Vertical scrolling still works when the finger starts on a row.
- The system back gesture from the screen edge is not stolen.
- The row closes after about 5 seconds, on scroll, and on opening another row.
- A row stays correct after its status changes (the old `SwipeRow` had a stale-state bug, see the comment in `SwipeRow.kt`).
- Check with TalkBack on once.

## Phase 5: Detail sheet

Risk: **Medium**

- The sheet receives the selected row's display data: emoji with badge, signed amount, share with struck full amount.
- Category, payment method and "Split with" are shown only. Editing happens on Add Transaction.
- Duplicate card shows both messages with each one's status, with the buttons from Phase 2.
- Buttons by status:
  - To review: Reject, Confirm. Confirm on money out opens Add Transaction (already what `onAcceptFromDetail` does); on money in it confirms straight away.
  - Confirmed: Undo (goes through the Phase 3 dialog) and Edit transaction (opens the existing edit screen).
  - Rejected: Undo.

Tests
- `onAcceptFromDetail` tests stay (debit navigates, credit confirms directly).
- New: sheet Undo on confirmed money out goes through the Phase 3 dialog.
- New: Edit transaction navigates with the linked transaction id.

Phone check: open the sheet for each status; confirm from the sheet lands on Add Transaction with the SMS filled in.

## Phase 6: Top bar, filters and empty states

Risk: **Low to Medium**

- Filter icon and a ⋮ menu go in the top bar (app bar actions live in `AppNavigation`). The menu holds Re-scan, Re-apply rules, and Delete month (red, behind the existing confirm dialog). The three outlined buttons are removed.
- Active filters (Direction, Category, Payment method) show as removable chips under the tabs; each ✕ resets only its own filter. The dot on the filter icon is dropped.
- Empty states with their own text: all caught up, empty month ("No bank SMS for October" with the re-scan action), and filters hide everything (with a Clear filters button).

Tests
- Removing one chip resets only that filter.
- Empty-state wording is chosen correctly: caught up vs empty month vs filters hide everything.
- Menu actions call the same view model functions as the old buttons.

Phone check: Delete month still asks first; re-scan fetches the viewed month; chips and counts match the filter sheet.

## Phase 7: Docs and final pass

Risk: **Low**

- `docs/06-backlog.md`: dated entry with the root causes found (Undo deleting splits, duplicates ignoring decided messages), the trade-offs accepted, and the items not built.
- Update `docs/04-messages-and-notifications.md` and `docs/08-pages-and-navigation.md`.
- Full pass in dark mode and at 1.3x font size; run the whole unit test suite.

## Not built

Restore snackbar, pull-to-refresh, "Accept all", "This week" and "Earlier" day buckets, any swipe change on Owed to you or Category detail.

## Order and dependencies

Phases 0 to 3 are independent and low risk, so they can ship first. Phase 5 needs Phases 2 and 3. Phase 4 can be built any time after Phase 1 but should be tested alone on the device. The uncommitted split-UI work in the repo touches `Transaction.kt`, the DAO and repositories, so commit or stash it before Phase 1 to keep diffs readable.
