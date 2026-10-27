# MandiSamiti Revamp — Flow 02: Sell Flow (बिक्री सौदा)

## 1. Flow Overview & Scope
The Sell Flow handles retail sales to direct consumers (Customer) and wholesale distribution to grain merchants (Merchant). Invoicing, tax liabilities, and mandatory fields depend strictly on the actor category and invoice mode.

---

## 2. Actor & Mode Rules Matrix

| Actor / Category | Allowed Mode | GSTIN & State Code | Mandi Tax (1.5%) & Cess (0.5%) | Commission % & Labour (₹/bag) | Multiple Crop Lots | Prapt Rashi (Received) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Customer (ग्राहक)** | **Normal Slip only** | Not Allowed | Not Applied (0%) | Commission % + Labour (₹/bag) | Yes (multiple variants at diff rates) | **Removed / Not Required** |
| **Merchant (व्यापारी)** | **Normal Slip** | Not Required | Not Applied (0%) | Commission % + Labour (₹/bag) | Yes (multiple variants at diff rates) | **Removed / Not Required** |
| **Merchant (व्यापारी)** | **Bill of Supply** | **Mandatory** (15-char GSTIN, 2-digit StateCode) | **Mandatory** (Mandi 1.5% + Cess 0.5%) | Labour (₹/bag) + Tulak | Yes (multiple variants at diff rates) | **Removed / Not Required** |

### Key Business Guardrails
1. **Bill of Supply Restriction:** Bill of Supply is strictly for **Merchants**, never for retail Customers. When "ग्राहक" (Customer) is selected, Bill of Supply is disabled/hidden.
2. **Normal + Customer:** Includes Customer Name/Mobile, Crop Name, Bags, Weight/bag, Extra weight, Rate, Commission %, and Labour charges. No GST or Mandi Tax.
3. **Normal + Merchant:** Same calculation and fields as Customer, categorized under Merchant ledger. No GST or Mandi Tax.
4. **Bill of Supply + Merchant:** Formal mandi invoice requiring Buyer GSTIN, State Code, Calendar Invoice Date, and multiple itemized crop variants with Mandi Fee (1.5%), Krishak Kalyan Cess (0.5%), and Labour/Tulak.
5. **No "Prapt Rashi":** Removed from deal entry screen. All transactions default to ledger debit / credit for subsequent passbook reconciliation.

---

## 3. Math Engine Specifications & Formulae

### Multi-Lot Aggregation
Each sale can contain $N \ge 1$ crop lots (e.g. Lokwan 20 bags @ ₹2600 + Sharbati 10 bags @ ₹3100).
- **Lot Weight (grams):** $W_i = (\text{bags}_i \times \text{weightPerBag}_i) + \text{extraWeight}_i$
- **Lot Gross (paisa):** $G_i = \text{round}\left(\frac{W_i \times \text{ratePaisaPerQuintal}_i}{100,000}\right)$
- **Total Weight (grams):** $W_{\text{total}} = \sum_{i=1}^N W_i$
- **Total Gross (paisa):** $G_{\text{total}} = \sum_{i=1}^N G_i$
- **Total Bags:** $B_{\text{total}} = \sum_{i=1}^N \text{bags}_i$

### Mode Calculations
1. **Normal Slip (Customer or Merchant):**
   - **Commission Paisa:** $\text{round}\left(\frac{G_{\text{total}} \times \text{commissionBps}}{10,000}\right)$
   - **Labour Paisa:** $B_{\text{total}} \times \text{labourPerBagPaisa}$
   - **Mandi Tax / Cess:** ₹0
   - **Total Receivable (देय राशि):** $G_{\text{total}} + \text{commissionPaisa} + \text{labourPaisa}$

2. **Bill of Supply (Merchant only):**
   - **Mandi Shulk (1.5%):** $\text{round}\left(\frac{G_{\text{total}} \times 150}{10,000}\right)$
   - **Krishak Kalyan Cess (0.5%):** $\text{round}\left(\frac{G_{\text{total}} \times 50}{10,000}\right)$
   - **Labour Paisa:** $B_{\text{total}} \times \text{labourPerBagPaisa}$
   - **Tulak Paisa:** $\text{tulakOverride} \mathbin{?} \text{defaultTulak}(W_{\text{total}})$
   - **Total Receivable (कुल देय):** $G_{\text{total}} + \text{mandiShulk} + \text{cess} + \text{labourPaisa} + \text{tulakPaisa}$

---

## 4. Test Cases Matrix

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-SELL-01** | Normal + Customer (Single Lot) | Customer "Ramesh Kumar", 10 bags @ 50kg, ₹2600/Qtl, 2% comm, ₹5/bag labour | Gross: ₹13,000, Comm: +₹260, Labour: +₹50, Total: ₹13,310 | **PASS on Device** |
| **TC-SELL-02** | Normal + Customer (Multi-Lot) | Lot 1: 10 bags @ ₹2600, Lot 2: 10 bags @ ₹2800, 2% comm, ₹5/bag labour | Gross: ₹27,000, Comm: +₹540, Labour: +₹100, Total: ₹27,640 | **PASS (JVM Unit)** |
| **TC-SELL-03** | Normal + Merchant | Merchant "Shree Agro", 20 bags @ ₹2500, 1.5% comm, ₹4/bag labour | Gross: ₹25,000, Comm: +₹375, Labour: +₹80, Total: ₹25,455, Category: Merchant | **PASS (JVM Unit)** |
| **TC-SELL-04** | Customer BOS Disabled | Party = Customer | "सप्लाई बिल (Bill of Supply)" chip is disabled ("सप्लाई बिल केवल व्यापारियों हेतु") | **PASS on Device** |
| **TC-SELL-05** | BOS + Merchant (Full Mandi Tax) | Merchant "Vijay Traders", GSTIN `23AAAAA0000A1Z5`, StateCode `23`, 20 bags @ ₹2600 (10 Qtl), Tulak ₹1/qtl, Labour ₹5/bag | Gross: ₹26,000, Mandi (1.5%): ₹390, Cess (0.5%): ₹130, Labour: ₹100, Tulak: ₹10, Total: ₹26,630 | **PASS (JVM Unit)** |
| **TC-SELL-06** | BOS Multi-Item Lots | Merchant "Shree Ganesh Traders", GSTIN `23AAAAA0000A1Z5`, StateCode `23`, Lot 1: 20 bags Sharbati @ ₹2600, Lot 2: 10 bags Chana @ ₹5200 | Gross: ₹52,000, Mandi (1.5%): ₹780, Cess (0.5%): ₹260, Labour: ₹150, Tulak: ₹15, Total: ₹53,205 | **PASS on Device** |
| **TC-SELL-07** | Validation Guardrails | Empty Buyer Name, Bags = 0, or (BOS without GSTIN / StateCode) | "सुरक्षित करें" disabled with explicit banner explaining why | **PASS on Device & JVM** |
| **TC-SELL-08** | No Prapt Rashi | Any Sell mode | No "प्राप्त राशि" input field; deal saves directly to ledger | **PASS on Device** |
| **TC-SELL-09** | Overselling Stock Warning | Total sale weight > available godown stock | Prominent warning banner displayed, transaction allowed with caution | **PASS on Device** |
| **TC-SELL-10** | Receipt & WhatsApp Formatting | Completed deal | S6 Receipt formats either Normal Slip or GST Bill of Supply with multi-lot breakdown | **PASS on Device** |

---

## 5. Screen Sequence & Navigation
1. **S4 (SellEntryScreen):**
   - Category Selector: `[ग्राहक (Customer)]` vs `[व्यापारी (Merchant)]`
   - Invoice Mode: `[सामान्य बिल (Normal)]` vs `[सप्लाई बिल (Bill of Supply)]` (BOS disabled if Customer)
   - Buyer Details Card: Name (Required), Phone (Optional 10-digit), GSTIN & State Code (Mandatory if BOS), Invoice Date (Calendar)
   - Multi-Crop Lots: itemized cards with crop quick-chips, bags, weight/bag, rate/quintal, and add/delete lot actions
   - Deductions / Charges: Commission % & Labour (Normal) or Mandi Tax + Cess + Labour + Tulak (BOS)
   - Live Math Summary Card & Validation Banner
2. **S5 (SellTransactionsListScreen):** Chronological log of all sales, searchable and filterable by Customer vs Merchant.
3. **S6 (SellTransactionReceiptScreen):** Printable GST Bill of Supply or Normal Buyer Slip with WhatsApp share.

---

## 6. Physical Device Test Evidence (Redmi Note 7, Serial: `d93c743`)

### Scenario A: Merchant + Bill of Supply Multi-Item Lot
- **Merchant Details:** Shree Ganesh Traders (`9826012345`), GSTIN `23AAAAA0000A1Z5`, State Code `23`.
- **Item 1:** गेहूं शरबती (20 बोरी @ 50 किग्रा = 10.0 क्विंटल @ ₹2,600/क्विंटल) = सकल ₹26,000.
- **Item 2:** चना (10 बोरी @ 50 किग्रा = 5.0 क्विंटल @ ₹5,200/क्विंटल) = सकल ₹26,000.
- **Live Calculations:**
  - कुल सकल राशि (Gross): ₹52,000
  - मंडी शुल्क (1.5%): + ₹780
  - कृषक कल्याण उपकर (0.5%): + ₹260
  - हमाली / मजदूरी (30 बोरी @ ₹5): + ₹150
  - तुलाई (15 क्विंटल @ ₹1): + ₹15
  - **कुल देय राशि (Total Receivable): ₹53,205**
- **Receipt Bill Number:** `#BOS-52034`
- **Screenshots:**
  - Form & Details: `docs/testing/evidence/sell/02d_merchant_info_filled.png`
  - Multi-Lot 1: `docs/testing/evidence/sell/02e_lot1_filled.png`
  - Multi-Lot 2: `docs/testing/evidence/sell/02f_lot2_filled.png`
  - Live Math Preview: `docs/testing/evidence/sell/02g_math_preview.png`
  - S6 Receipt Screen: `docs/testing/evidence/sell/03_sell_receipt_screen.png`
  - S5 Transaction List: `docs/testing/evidence/sell/04_sell_list_screen.png`

### Scenario B: Customer + Normal Slip Single Lot
- **Customer Details:** Ramesh Kumar (`9876543210`), Category: `ग्राहक (Customer)`.
- **Bill Mode Restriction:** `सप्लाई बिल` disabled ("सप्लाई बिल केवल व्यापारियों हेतु").
- **Item 1:** गेहूं लोकवान (10 बोरी @ 50 किग्रा = 5.0 क्विंटल @ ₹2,600/क्विंटल) = सकल ₹13,000.
- **Live Calculations:**
  - कुल सकल राशि (Gross): ₹13,000
  - कमीशन जोड़ (+ 2%): + ₹260
  - मजदूरी जोड़ (+ ₹5/बोरी): + ₹50
  - मंडी कर / उपकर: ₹0 (Not Applied)
  - प्राप्त राशि: Not Required / Removed
  - **कुल देय राशि (Total Receivable): ₹13,310**
- **Receipt Bill Number:** `#cdfb6a`
- **Screenshots:**
  - Customer Gating & Form: `docs/testing/evidence/sell/07_customer_sell_opened.png`
  - Customer Lot Filled: `docs/testing/evidence/sell/08_customer_lot_filled.png`
  - Live Math Preview: `docs/testing/evidence/sell/09_customer_math_preview.png`
  - S6 Receipt Screen: `docs/testing/evidence/sell/10_customer_receipt_screen.png`
  - S5 Transaction List (Both Cards): `docs/testing/evidence/sell/11_sell_list_both_transactions.png`

