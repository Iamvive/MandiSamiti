# Entry Deletion & Correction Standard (Audit Integrity)

← [Docs index](../MANDISAMITI-MASTER-SPEC.md)

In traditional Mandi Aadhat, transactions cannot simply "disappear" without an audit record:

1. **Soft-Delete / Void Mechanism:**
   - Any transaction (Deal, Cash In, Cash Out, Settlement) has a `isVoid: Boolean` and `voidReason: String?`.
   - When user taps **"हटाएं / रद्द करें"**, a bottom sheet requests confirmation and a simple reason (*तौल त्रुटि*, *गलत प्रविष्टि*, *सौदा निरस्त*).
2. **Instant Balance Recalculation:**
   - The party's running balance (`balancePaisa`) and daily cash drawer (`inHandCashDrawerPaisa`) are automatically recalculated immediately via SQLDelight reactive queries.
3. **Visual Distinction:**
   - Voided entries appear in ledger with strikethrough and a muted status tag (`रद्द प्रविष्टि`), or are hidden under an "Unvoid / View Cancelled" toggle.
4. **Append-Only Sync Engine:**
   - Revisions are tracked in `entryRevisionEntity` and synced as delta operations to backend `EntryRevision` table.
