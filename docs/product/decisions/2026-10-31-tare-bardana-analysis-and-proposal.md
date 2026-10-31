# Mandi Domain & UX Note: काट / बारदाना (Tare / Bardana) Analysis & Proposal

**Document Type:** Business Domain Reference & Owner Discussion Memo  
**Target Project:** `projects/MandiSamiti` (KMP Alpha)  
**Date:** 2026-10-31  
**Author:** Antigravity (Pair Programming with Vivek)  

---

## 1. What is Tare / Bardana (काट / बारदाना)?

In agricultural grain markets (मंडी), three weight concepts govern every trade settlement slip:

1. **Gross Weight (सकल वजन):**
   The combined weight measured directly on the scale (Dharmkanta/Kanta) — Grain + Packaging bags (Bardana) + Moisture/Refraction/Dust.
2. **Tare Weight (काट या बारदाना):**
   The weight deducted for packaging material (empty gunny/jute bags, plastic HDPE sacks) or standard committee allowance for moisture/dirt.
3. **Net Payable Weight (शुद्ध वजन):**
   The actual weight of agricultural produce for which the farmer receives payment.

$$\text{Net Weight (शुद्ध वजन)} = \text{Gross Weight (सकल वजन)} - \text{Tare Weight (काट / बारदाना)}$$

---

## 2. Real-World Mandi Scenarios

### Scenario A: Empty Bag Tare (जूट/प्लास्टिक बोरी की काट)
* A farmer unloads **100 bags of Wheat**.
* The scale reading is **50.00 Quintals (5,000 kg)** gross.
* In physical trading, an empty jute bag (कट्टा) weighs approximately **1 kg (or 800–1000g)**.
* 100 bags $\times$ 1 kg = **100 kg = 1.00 Quintal** packaging weight.
* Net Wheat = $50.00 - 1.00 = 49.00\text{ Quintals}$.
* The farmer is paid for 49.00 Quintals.

### Scenario B: Refraction / Moisture Deduction (कमी / धूल / नमी काट)
* During wet harvest periods or high-moisture arrivals (e.g. wet paddy or winter mustard), buyers and commission agents agree on a fixed deduction (e.g., 35 kg or 50 kg per trolley) to offset water weight.
* If a trolley weighs 40.00 Quintals and a flat tare of **0.35 Quintals (35 kg)** is applied:
  $$\text{Net Weight} = 40.00 - 0.35 = 39.65\text{ Quintals}$$

### Scenario C: Pre-deducted Scale Weighing (कांटे पर पहले से शुद्ध वजन)
* Modern electronic weighbridges and computerized mandi weigh slips often automatically zero out vehicle/bag tare, so the ticket already shows the net grain weight.
* In this case, **Tare should be 0.00 Quintals**.

---

## 2a. Why Does the App Currently Show 0.35?
* In `TradeSettings.kt`, the field `defaultTareGrams` was originally given a default constant of `35000L` (35 kg = 0.35 quintals).
* This was carried into `BuyEntryFormState` and `SellEntryFormState` as the initial text value:
  ```kotlin
  var tareQuintals by mutableStateOf(
      tradeSettings?.let { MandiMathEngine.gramsToQuintalsInputString(it.defaultTareGrams) } ?: ""
  )
  ```
* Because it is typed into the text field at startup, the user is forced to backspace it on every transaction where tare does not apply.

---

## 3. Other Fields Suffering from the Same Friction

| Field | Current Pre-fill | Problem | Proposed Fix |
| :--- | :--- | :--- | :--- |
| **Tare / Bardana** | `"0.35"` | Unwanted 35 kg deduction unless deleted | **Empty `""`**; gray hint `"0"` |
| **Weight per Bag** | `"50"` | Must delete `"50"` if bags are 40 kg, 60 kg, or 100 kg | **Empty `""`**; placeholder `"50 kg"` |
| **Crop Name** | `"गेहूं"` | Typing "चना" produces "चनागेहूं" or requires deleting first | **Empty `""`**; placeholder `"फसल नाम"`; select via 1-tap quick chips |
| **Commission %** | `"0.7"` | Normal default, but hard to change | Auto-select all text on focus so typing overwrites without backspacing |

---

## 4. Agenda & Questions for Discussion with the Business Owner (आढ़ती)

When discussing this with the shop owner or head Munim:

1. **Tare Default Setting:**
   * *"क्या आपकी मंडी में हर किसान की पर्ची पर 35 किलो (या कोई तय काट) अनिवार्य रूप से कटती है, या फिर ज्यादातर सौदों में सीधे शुद्ध कांटे का वजन लिखा जाता है?"*
   * *सुझाव (Recommendation):* डिफ़ॉल्ट काट को **0 किग्रा** रखें। जिस पर्ची पर काट लागू हो, केवल उसी में मुनीम संख्या दर्ज करे।

2. **प्रति बोरी काट बनाम एकमुश्त काट (Per-Bag vs Flat Weight Tare):**
   * *"क्या आप काट प्रति बोरी (जैसे 1 किलो/बोरी) काटते हैं या पूरी ट्रॉली पर एकमुश्त क्विंटल (जैसे 0.35 क्विंटल) काटते हैं?"*
   * अगर प्रति बोरी है, तो ऐप बोरी संख्या देखकर खुद वजन घटा सकता है।

3. **फॉर्म प्री-फिल वरीयता (Form Cleanliness):**
   * *"क्या आप चाहते हैं कि नया पर्चा बनाते समय वजन और फसल वाले डिब्बे खाली रहें (ताकि आपको पुराना लिखा मिटाना न पड़े)?"*

