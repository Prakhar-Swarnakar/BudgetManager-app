# Messages and Notifications

## What counts as a message

A message is created from an incoming bank SMS that looks like a **debit** (money spent). Other SMS such as OTPs, promotions, balance alerts, and credits are ignored in v1 (credit handling is proposed as a backlog item).

Because several banks are used, the app cannot rely on one fixed SMS format. Parsing should look for common patterns such as "debited", "spent", "paid", and "UPI", along with an amount and a merchant name.

**If the amount or merchant cannot be read:** a message that clearly looks like a spend (it has a debit word and a rupee amount) but cannot be fully parsed is still saved. It appears on the Messages page with its original text, and the missing fields are left blank for the user to fill in when accepting. A message with no debit word, or with an OTP, is never saved. OTP text is never stored.

## Message fields

| Field | Notes |
|---|---|
| Sender | The SMS sender ID, such as a bank's short code |
| Amount | Extracted from the SMS. Blank if it could not be read |
| Merchant | Extracted from the SMS; used as the default note |
| Date and time | From the SMS |
| Original text | Full SMS text, shown when the message is opened |
| Status | Not assigned, accepted, or rejected |
| New flag | True until the user has opened the Messages page and left it. Drives the red circle on the tab and the bold text and blue dot on the row |
| Unique key | Built from the SMS's own id, time, and text, so the same SMS is never saved twice |
| Suggested category | From keyword matching, may be empty |
| Linked transaction | Set when the message is accepted and saved. Cleared, and the status returns to Not assigned, if that transaction is deleted |

## Statuses and colours

| Status | Colour | Meaning |
|---|---|---|
| Not assigned | White | Received, no decision yet |
| Accepted | Green | User accepted it and saved a transaction |
| Rejected | Red | User rejected it; no transaction was created |

## Messages page

- Reachable from the app's navigation. Its icon shows a **small red circle** when there are new messages.
- Lists all received messages, newest first *(assumed)*.
- Each row is coloured by its status.
- Accepted and rejected messages stay in the list. The filter chips (All, Not assigned, Accepted, Rejected) keep the list manageable. Archiving old messages is in the backlog.
- The red circle on the tab clears when the user opens the Messages page. Rows keep their bold text and blue dot until the user leaves the page, so they can see which ones were new.
- The Home line "N messages to review" counts messages that are still **Not assigned**, which is different from the "new" flag.
- **Possible-duplicate warning** (built 2026-10-01, `DetectPossibleDuplicates`): a Not assigned row gets a small warning badge when another Not assigned message has the same amount, a different sender, and arrived within 10 minutes - e.g. a bank debit alert and a UPI app's own confirmation for the same payment. The detail sheet names which other message it might match. Only Not assigned messages are considered - once a message is Accepted or Rejected the user has already decided, so flagging it again would just be noise. This never auto-rejects anything; the user still chooses.
- **Category shown before accepting** (built 2026-10-02): the row's category slot isn't Accepted-only - a Not assigned row shows its keyword suggestion there too, in the same spot and style a real category appears in once Accepted, so you can see what a spend would be filed under without opening it. Rejected never shows one, since it isn't a real spend.
- **Split by month, with per-month fetch and rule re-run** (built 2026-10-02): the page has its own month selector (same control as Home), always showing the currently viewed month's messages only - filter chips, counts, and rows are all scoped to it. The selector cannot move past the real current month. Two buttons sit under it:
    - **Fetch SMS** - scans the phone's SMS inbox for the viewed month's date range specifically, independent of the automatic catch-up marker that runs on app open. Safe to tap more than once; an already-saved message is skipped by its dedupe key, same as any other ingest path. This replaced the old debug-only "Import today's SMS" / "Add test message" buttons.
    - **Run rule** - re-applies the current keyword rules to every **Not assigned** message in the viewed month, updating [`suggestedCategoryId`](../app/src/main/java/com/budgetmanager/app/core/model/SmsMessage.kt) and (built 2026-10-02) [`suggestedTaxonomy`](../app/src/main/java/com/budgetmanager/app/core/model/SmsMessage.kt) - the separate payment-method suggestion. It never touches status, and never touches an Accepted or Rejected message - those have already been decided (or weren't a real spend) and are left exactly as they are. Useful after adding or editing a keyword rule, to fix old suggestions without re-deciding anything.
    - **Clear month** - removes every message in the viewed month, regardless of status, after a confirmation naming the month. A linked transaction is kept, only unlinked (`source_message_id` set null) - its amount and category spend are untouched. For re-importing a month cleanly via Fetch SMS without old rows lingering.
    - Possible-duplicate detection (above) still runs against every message, not just the viewed month's - a payment can straddle a month boundary (e.g. 11:58pm vs 12:01am).
- **Exact-duplicate fix: sender dropped from the dedupe key** (built 2026-10-02): real on-device data showed the same bank re-sending an identical debit SMS through more than one registered sender header minutes apart (e.g. the same notification arriving as both `BG-XXXXXX-S` and `JD-XXXXXX-S`) - `DedupeKey` previously included the sender, so each header produced a different key and every copy got saved as a separate message. The key now only uses the day and the body text, since two genuinely different same-day transactions already differ in body (amount, merchant, a reference number) without the sender's help.

### Gestures

Confirmed and built in M3/M4 (2026-09-25). Only a **Not assigned** message can become Accepted or Rejected; there is no direct Accepted-to-Rejected move or reverse. Accepted and Rejected each only revert to Not assigned. This is stricter than the original plan below (row 7 in [07-open-questions.md](07-open-questions.md)) and replaces it, to keep the state machine symmetric and to stop an accepted message's transaction from ever being orphaned by a status change that bypasses deleting it.

| Row status | Swipe right (start-to-end) | Swipe left (end-to-start) |
|---|---|---|
| Not assigned | Opens Add Transaction, pre-filled. Becomes Accepted once saved | Reject. A short Undo bar appears |
| Accepted | No effect | Reverts to Not assigned (deletes the linked transaction) |
| Rejected | Reverts to Not assigned | No effect |

The reveal strip under the row always matches what that swipe will really do: green check / red cross on a Not assigned row, a neutral grey "undo" icon on the one live direction for Accepted or Rejected, and nothing on a no-op direction.

Tap opens the message and shows the full SMS, with Accept and Reject buttons on the detail sheet. Those buttons only act while the message is Not assigned; on an already-decided message they show but do nothing, since the only way back is the revert swipe above.

### Accepting

Accepting opens the **Add Transaction** page with these fields pre-filled:

- Amount from the SMS
- Date from the SMS
- Note set to the merchant
- Category set to the keyword suggestion, or empty if nothing matched

The user can change any field. The message becomes green when the transaction is saved.

## Category suggestion

- A keyword list maps words to categories, for example a food-delivery merchant to Food. The
  longest matching keyword wins (`CategorySuggester`), so a more specific word beats a shorter
  one that happens to also be a substring.
- The suggestion is only a pre-fill. The user always sees it and can change it.
- If nothing matches, no category is suggested.
- **Learning from choices** (built 2026-10-01): saving an accepted message's transaction remembers
  its merchant as a keyword mapped to whichever category the user actually picked - including
  when that overrides the original suggestion - so the same merchant suggests correctly next
  time. Merchant text under 3 characters is never learned, since a very short word is more likely
  to misfire against an unrelated future message than to help.
- **User-editable keyword rules** (built 2026-10-01, grouped layout 2026-10-02, moved to the side
  panel 2026-10-02): a "Category rules" page in the side panel, grouped by category - each one is a section showing its words as
  removable chips plus a "+ add" chip, including a category with none yet. Tapping a chip reassigns
  its category; the keyword itself can't be edited in place - see "Editable keyword rules" in
  [06-backlog.md](06-backlog.md).

## Payment method taxonomy (built 2026-10-02)

A second, independent keyword-rule engine alongside category suggestion - same shape
(`TaxonomySuggester`, same longest-match-wins algorithm), a separate table
(`taxonomy_keyword_rule`), so a word like "zomato" can suggest a category and a payment method
at once, from two different rules. Suggested at the same ingest time as the category
(`SmsReceiver`, `DefaultInboxScanner`), stored on the message as `suggestedTaxonomy`, and
refreshed by "Run rule" the same way. The 8 values are fixed (UPI, Credit Card, Debit Card,
Credit Card via UPI, Bank Transfer, Wallet, Cash, Other), so its own "Taxonomy rules" page in the
side panel has no create/rename step the way Category rules does - just adding or removing
keywords under each already-existing value. **Shown on the row** (built 2026-10-02): the same
slot/style as category, directly underneath it - an Accepted row's real transaction taxonomy, or
a Not assigned row's keyword suggestion, same rule as category (Rejected never shows one). See
"Payment method taxonomy" in [06-backlog.md](06-backlog.md).

## Notifications

### New messages

- When new spend messages arrive, the app shows a simple notification such as "3 new spends detected".
- Tapping it opens the Messages page *(assumed)*.
- There are no action buttons in v1 (backlog).
- Grouping many messages that arrive together (SMS bursts) is in the backlog.

### Budget alerts

- **80% reached:** a notification when a category's spending reaches 80% of its budget.
- **Overspent:** a notification when spending in a category goes over its budget.
- Details are in [05-budget-rules.md](05-budget-rules.md).

## Permissions the app needs

- Read SMS.
- Post notifications.
- Being exempt from battery optimisation may be needed on some phones so notifications are not delayed.
