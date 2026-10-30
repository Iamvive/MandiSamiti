# MandiSamiti Pilot Rehearsal & Sheet Parity Report

**Date:** 2026-10-30  
**Target:** Redmi Note (Physical Device `d93c743`), Staging VPS (`http://64.227.142.180:8060`)  
**Artifact:** `exports/mandisamiti-pilot-debug.apk` (21,433,089 bytes)  
**Status:** PASS (All 5 Golden Cases matched to the rupee; GSTIN/Licence verified on-device and round-trip synced)

---

## 1. Scope & Execution Summary
Following tasks U1 and P1 in `prompts/mandisamiti-sheet-parity-tasks.md` and sheet math in `vaults/Vivek-K/wiki/concepts/mandisamiti-sheet-math.md`:
1. **GSTIN & Mandi Licence in Buyer Forms**: Added to Add Party dialog and Edit Party dialog in `KhataLedgerTabScreen.kt` and `PartyLedgerScreen.kt`. Tested on-device: hidden for farmers, visible with 15-char uppercase validation and max 50-char licence for buyers.
2. **Buyer Detail Page Display**: Header card displays GSTIN and Mandi Licence; live editing updates local database and syncs to staging backend (`parties.gstin` and `parties.mandi_license`).
3. **Bill of Supply Prefill**: Auto-prefills GSTIN and State Code when selecting a buyer or switching mode to Bill of Supply.
4. **On-Device 5 Golden Cases Sheet Parity**: Executed on the physical Redmi device, matching live preview and calculations to the exact rupee.

---

## 2. Five Golden Cases On-Device Verification Evidence

### Case 1 (Farmer Slip, Standard 0.7% Commission)
- **Input**: 45 bags, 49 kg/bag, extra 29 kg, Rs 4,001/qtl, commission 0.7%, labour Rs 10/bag.
- **Rules applied**:
  - Total weight: \(45 \times 49 + 29 = 2,234\) kg = 22.34 qtl.
  - Gross cut down to whole rupee: \(22.34 \times 4001 = 89,382.34 \rightarrow\) **₹89,382**.
  - Billed bags: extra weight \(29 > 5\) kg adds 1 bag \(\rightarrow 46\) bags.
  - Labour: \(46 \times 10 =\) **₹460**.
  - Final rounded to nearest Rs 10: \(89,382 - 460 - (89,382 \times 0.7\%) = 88,296.33 \rightarrow\) **₹88,300**.
  - Commission absorbs rounding: \(89,382 - 460 - 88,300 =\) **₹622**.
- **On-Device Verification**:
  - Gross Amount: **₹89,382** (Exact Match)
  - Commission: **- ₹622** (Exact Match)
  - Labour: **- ₹460** (Exact Match)
  - Net Payable: **₹88,300** (Exact Match)

### Case 2 (Farmer Slip, Extra Weight Threshold Rule)
- **Input**: 31 bags, 55 kg/bag, extra 5 kg, Rs 2,250/qtl, commission 0.7%, labour Rs 10/bag.
- **Rules applied**:
  - Total weight: \(31 \times 55 + 5 = 1,710\) kg = 17.10 qtl.
  - Gross: \(17.10 \times 2250 =\) **₹38,475**.
  - Billed bags: extra weight exactly 5 kg does NOT add a bag \(\rightarrow 31\) bags.
  - Labour: \(31 \times 10 =\) **₹310**.
  - Final rounded to nearest Rs 10: \(38,475 - 310 - (38,475 \times 0.7\%) = 37,895.67 \rightarrow\) **₹37,900**.
  - Commission absorbs rounding: \(38,475 - 310 - 37,900 =\) **₹265**.
- **On-Device Verification**:
  - Gross Amount: **₹38,475** (Exact Match)
  - Commission: **- ₹265** (Exact Match)
  - Labour: **- ₹310** (Exact Match)
  - Net Payable: **₹37,900** (Exact Match)

### Case 3 (Farmer Slip, 1.0% Commission Variant)
- **Input**: 12 bags, 50 kg/bag, extra 41 kg, Rs 2,220/qtl, commission 1.0%, labour Rs 10/bag.
- **Rules applied**:
  - Total weight: \(12 \times 50 + 41 = 641\) kg = 6.41 qtl.
  - Gross: \(6.41 \times 2220 = 14,230.20 \rightarrow\) **₹14,230**.
  - Billed bags: extra weight \(41 > 5\) kg adds 1 bag \(\rightarrow 13\) bags.
  - Labour: \(13 \times 10 =\) **₹130**.
  - Final rounded to nearest Rs 10: \(14,230 - 130 - (14,230 \times 1.0\%) = 13,957.70 \rightarrow\) **₹13,960**.
  - Commission absorbs rounding: \(14,230 - 130 - 13,960 =\) **₹140**.
- **On-Device Verification**:
  - Gross Amount: **₹14,230** (Exact Match)
  - Commission: **- ₹140** (Exact Match)
  - Labour: **- ₹130** (Exact Match)
  - Net Payable: **₹13,960** (Exact Match)

### Case 4 (Buyer Bill, Bill of Supply)
- **Input**: 35 bags, 49 kg/bag, Rs 3,751/qtl, Arat 1.5%, Mandi Shulk 1.5%, Tulak Rs 1/qtl.
- **Rules applied**:
  - Total weight: \(35 \times 49 = 1,715\) kg = 17.15 qtl.
  - Base / Gross: \(17.15 \times 3751 =\) **₹64,329.65**.
  - Arat (1.5%): \(64,329.65 \times 0.015 =\) **₹964.94**.
  - Mandi Shulk (1.5%): \(64,329.65 \times 0.015 =\) **₹964.94**.
  - Tulak (Rs 1/qtl): \(17.15 \times 1.00 =\) **₹17.15**.
  - Krishak Kalyan Cess: 0% (BOS has no Krishak Cess).
  - Total Receivable: \(64,329.65 + 964.94 + 964.94 + 17.15 =\) **₹66,276.68**.
- **On-Device Verification**:
  - Total Gross Amount: **₹64,329.65** (Exact Match)
  - Mandi Tax (1.5%): **+ ₹964.94** (Exact Match)
  - Krishak Cess (0.5%): **+ ₹0** (Exact Match)
  - Tulak: **+ ₹17.15** (Exact Match)
  - Total Receivable: **₹66,276.68** (Exact Match)

### Case 5 (Buyer Bill, Cash Memo)
- **Input**: 8 bags, 49 kg/bag, Rs 3,901/qtl, Arat 1.5%, Tulak Rs 1/qtl nominal.
- **Rules applied**:
  - Total weight: \(8 \times 49 = 392\) kg = 3.92 qtl.
  - Base / Gross: \(3.92 \times 3901 =\) **₹15,291.92**.
  - Arat (1.5%): \(15,291.92 \times 0.015 =\) **₹229.38**.
  - Unrounded sum: \(15,291.92 + 229.38 + 3.92 = 15,525.22\).
  - Final cut down to whole rupee: **₹15,525**.
  - Tulak absorbs rounding difference: \(15,525 - 15,291.92 - 229.38 =\) **₹3.70**.
- **On-Device Verification**:
  - Total Gross Amount: **₹15,291.92** (Exact Match)
  - Add Commission (+): **+ ₹229.38** (Exact Match)
  - Total Receivable: **₹15,525** (Exact Match)

---

## 3. GSTIN & Mandi Licence Local & Server Sync Round-Trip
- Created `TEST Vyapari` (Mathura) with GSTIN `09AAAAA0000A1Z5` and Mandi Licence `LIC-9988`.
- Confirmed on phone detail screen: displayed in "Tax & Licence Details" card.
- Confirmed via Postgres query on staging database (`mandisamiti_staging` on VPS):
  ```
  SELECT id, name, role, gstin, mandi_license, is_deleted FROM parties WHERE name='TEST Vyapari';
  ```
  Row retrieved: `c9478002-91d7-492b-99c1-f4008d177e0b | TEST Vyapari | BUYER | 09AAAAA0000A1Z5 | LIC-9988 | 0`
- Cleaned up: TEST party wiped from staging database and device storage cleared.

---

## 4. Test Suite Parity
- All 156 `:composeApp:jvmTest` unit tests pass.
- All `:core-domain:jvmTest` math parity tests pass.
- Gradle build and packaging clean.
