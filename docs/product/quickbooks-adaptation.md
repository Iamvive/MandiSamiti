# QuickBooks Mechanics Adaptation & Brainstorming (Phase 7 Backlog)

← [Docs index](../MANDISAMITI-MASTER-SPEC.md) · Status: [feature status](feature-status.md)

QuickBooks is a proven benchmark for SME finance, but its mechanics must be fundamentally adapted for the chaotic, low-literacy, high-velocity Indian APMC Mandi environment:

## A. Non-Negotiable Mandi Guardrails (Anti-QuickBooks Rules)
1. **Zero Double-Entry Jargon:** Never use *Debit*, *Credit*, *Chart of Accounts*, or *A/R*. Everything is **जमा (In)** vs **नामे / निकासी (Out)**.
2. **Sub-5-Second Touch Time:** 98% of interactions happen on the auction floor on low-end Android hardware. No multi-screen forms.
3. **100% Offline Resilience:** Must function flawlessly under corrugated tin shed roofs with zero cellular signal, syncing cursor deltas when reconnected.
4. **Senior Ergonomics (25–65):** Minimum 48dp tap targets, large numerals, voice soundbox confirmation.

## B. Core Features Slated for Brainstorming
1. **A/R Aging Adapted → "उधारी उम्र" (Overdue Aging Badges & Nudge):**
   - Party cards show visual aging flags: `7+ दिन` or `15+ दिन से बाकी`.
   - 1-tap WhatsApp nudge button generating a polite, formal Hindi statement with total balance.
2. **Daily Close-Out Adapted → "शाम का हिसाब व गल्ला लॉक" (Daily Close & Lock):**
   - At 6:00 PM, owner enters physical cash count.
   - App computes discrepancy (हिसाब बराबर / फालतू / कमी), generates daily tally card for WhatsApp, and optionally locks the day with MPIN to prevent unauthorized tampering.
3. **Expense Categorization Adapted → "मंडी खर्च चिप्स" (1-Tap Galla Expense Tags):**
   - Quick-select chips on Cash Out: *हम्माली (Hammali)*, *भाड़ा (Freight)*, *चाय-नाश्ता (Refreshments)*, *पल्लेदारी (Weighing labor)*, *मंडी सेस (Mandi Tax)*.
