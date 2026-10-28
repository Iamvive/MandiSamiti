# MandiSamiti Revamp — Flow 01: Buy Flow (खरीद सौदा)

## 1. Flow Overview & Scope
The Buy Flow covers grain procurement from farmers at the mandi auction platform (आढ़त खरीद). It calculates precise 64-bit integer paisa totals, deducts commission and labour, accounts for advance payments, persists to the local Room database, queues for V2 synchronization, and generates a formatted Hindi WhatsApp receipt.

### Screen Sequence
1. **S1 (BuyEntryScreen):** Input bags, weights, rate per quintal, commission %, labour per bag, and advance. Live preview card updates on every keystroke.
2. **S2 (BuyTransactionsListScreen):** Chronological log of all purchase transactions with search filter and quick-add FAB.
3. **S3 (BuyTransactionReceiptScreen):** Printable / shareable kisan hisab parcha (किसान हिसाब पर्चा) with WhatsApp text export.

---

## 2. Calculation & Math Engine Assertions
All monetary math is executed in integer paisa using `MandiMathEngine` and `BuySellMath.calculateBuy`. Zero floating-point rounding errors are permitted.

### Math Formulae
- **Total Weight (grams):** `(bags * weight_per_bag_grams) + extra_weight_grams`
- **Total Quintals:** `total_weight_grams / 100,000.0`
- **Gross Paisa:** `round((total_weight_grams * rate_paisa_per_quintal) / 100,000)`
- **Commission Paisa:** `round((gross_paisa * commission_bps) / 10,000)`
- **Labour Paisa:** `bags * labour_per_bag_paisa`
- **Net Payable Paisa:** `gross_paisa - commission_paisa - labour_paisa - advance_paid_paisa`

### Worked Example Verification Matrix
| Parameter | Value | Engine Internal |
| :--- | :--- | :--- |
| Bags (`bags`) | 10 | 10L |
| Weight per Bag (`weightPerBagKg`) | 50 kg | 50,000 grams |
| Extra Weight (`extraWeightKg`) | 5 kg | 5,000 grams |
| Total Weight | 505 kg (5.05 Qtl) | 505,000 grams |
| Rate per Quintal (`ratePerQuintal`) | ₹2,500.00 | 250,000 paisa |
| Gross Total | ₹12,625.00 | 1,262,500 paisa |
| Commission % (`commissionPercent`) | 2.0% (200 bps) | ₹252.50 (25,250 paisa) |
| Labour per Bag (`labourPerBag`) | ₹5.00 / bag | ₹50.00 (5,000 paisa) |
| Advance Paid (`advancePaid`) | ₹1,000.00 | 100,000 paisa |
| **Net Payable (देय राशि)** | **₹11,322.50** | **1,132,250 paisa** |

---

## 3. Test Cases Matrix

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-BUY-01** | Standard Purchase | 10 bags, 50kg, ₹2500/Qtl, 2% comm, ₹5/bag labour | Gross: ₹12,500, Comm: ₹250, Labour: ₹50, Net: ₹12,200 | **PASS (Verified on Device)** |
| **TC-BUY-02** | Extra Weight & Advance | 10 bags, 50kg, 5kg extra, ₹2500/Qtl, 2% comm, ₹5/bag labour, ₹1000 adv | Gross: ₹12,625, Net: ₹11,322.50 | **PASS (Verified on Device)** |
| **TC-BUY-03** | Zero / Incomplete Input | All fields empty or bags = 0 | "सुरक्षित करें" disabled, warning banner displayed, no crash | **PASS (Verified on Device)** |
| **TC-BUY-04** | Advance Exceeds Gross | 1 bag, 50kg, ₹1000/Qtl, Advance ₹5000 | Warning banner displayed, save disabled | **PASS (Verified on Device)** |
| **TC-BUY-05** | Multi-Crop Lots | Lot 1: 10 bags @ ₹2800, Lot 2: 20 bags @ ₹2450 | Total 30 bags, 15.0 Qtl, Gross: ₹38,500, Comm: ₹770, Labour: ₹150, Net: ₹37,580 | **PASS (Verified on Device)** |
| **TC-BUY-06** | Room DB Persistence | Save multi-lot transaction | Row inserted in `buy_entries`, `remarks` holds multi-lot JSON, sync_status = 0 | **PASS (Verified on Device)** |
| **TC-BUY-07** | Receipt View & Multi-Item | Open receipt from S1 save or S2 tap | Itemized lots rendered cleanly with Hindi WhatsApp share | **PASS (Verified on Device)** |
| **TC-BUY-08** | List Screen Display | S2 Buy List | Displays slip #, bags, total quintal, and pending net payable | **PASS (Verified on Device)** |

---

## 4. Hardware Verification Evidence (Redmi Note 7 `d93c743`)

The entire flow was executed and verified on physical hardware running Android 10:
- **S1 Home Screen:** `docs/testing/evidence/buy/01_home_screen.png`
- **S1 Buy Entry Initial:** `docs/testing/evidence/buy/02_buy_entry_opened.png`
- **S1 Multi-Lot Entry & Live Math:** `docs/testing/evidence/buy/03b_buy_entry_multi_lot_filled.png`
- **S3 Receipt with Multi-Crop Breakdown:** `docs/testing/evidence/buy/04_buy_receipt_screen.png`
- **S2 Buy Transactions List:** `docs/testing/evidence/buy/05_buy_list_screen.png`
1. Launches `com.appwork.mandisamiti/.MainActivity`.
2. Triggers Speed Dial FAB -> clicks "खरीद".
3. Fills Farmer Phone, Bags (10), Weight (50), Rate (2500), Commission (2), Labour (5).
4. Asserts Live Preview displays Net Payable = ₹12,200.
5. Clicks "सुरक्षित करें" button.
6. Verifies S3 `BuyTransactionReceiptScreen` is displayed with receipt details.
7. Taps Back to S2 `BuyTransactionsListScreen` and verifies entry in list.
8. Captures step screenshots in `projects/MandiSamiti/docs/testing/evidence/buy/`.
