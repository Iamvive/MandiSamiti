# Spare crop (khaate) entries in Stock (design, draft)

Status: draft from brainstorming. Not built. Needs Vivek's review before writing-plans.

## Problem
At day end there is sometimes spare crop that is bought or sold without a real party. Today these have to be forced into a normal Buy or Sell with a party. Vivek wants them recorded directly in Stock, tagged as khaate, with no phone number, no party, no commission and no labour.

## Behaviour (as described by Vivek)
- Works for both buy and sell.
- Same entry flow as a normal buy or sell, with commission and labour at zero.
- No party and no phone number. The entry is tagged to a crop (Gehu, Dhaan, Jinse and others) and marked khaate.
- It goes to Stock, not Galla. It does not touch any party's khata balance.

## Chosen approach
- A spare entry is a Stock movement with `crop` and a `khaate = true` flag. Commission and labour are stored as zero.
- Stock already works per commodity (in, out, balance, daily movement view), and commodities are free-form, so more than three crops needs no new work. New crops are added from the entry form, not Settings.
- Assumed direction, to confirm: spare buy adds to stock (in), spare sell reduces stock (out).
- Daily view only. No running per-crop khaate balance across days.
- Spare entries appear in the Daily Movement list with a khaate tag, and in the day-end close-out totals.

## Edit and void
Critical in Adaat ("everything can go wrong"). Spare entries get the full edit trail and soft void with visible revision history, same as other entries.

## Out of scope for now
- Running per-crop khaate balance across days.
- Galla (cash) movement for spare entries.
- Any change to party khata balances.

## Open items
1. Decided: spare buy = stock in, spare sell = stock out.
2. Decided: spare entries carry rate and money amount (amount = quantity x rate, no commission or labour). Decided: in close-out the amount counts under Procured (Mohari), never under Payable, because no party is owed. The gross amount is labelled Asal. Stock means the Godown screen. No existing Mohari label in strings.xml, so it is a new string (Hindi first, English alongside).
3. Decided (option B): khaate quantity is shown on its own line per crop in the Godown, apart from the normal balance. Example: Gehu 100 bags normal, Khaate 5 bags. The physical count (stock audit) takes two inputs, normal bags and khaate bags, and compares each to its own line. Decided: spare entries only move the khaate line, normal entries only move the normal line. A spare sell larger than the khaate line is allowed with a warning only (line can go negative), no block. Compare normal Sell, which validates stock (feature sell-validates-stock); spare sell deliberately does not block.

## Verification plan (for when built)
- Unit: spare buy increases and spare sell decreases the commodity balance; commission and labour are zero; no party id required.
- Unit: day-end close-out includes spare entries; edit and void create revisions and reverse the stock effect.
- Device: entry flow, Daily Movement khaate tag, light and dark, 390px, per NGDL §5.7.
