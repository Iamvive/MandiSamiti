# MandiSamiti — App-Level Validation & Form Entry Best Practice Rules

## 1. Core Principles
All financial and inventory entry forms in MandiSamiti (Buy, Sell, Lend, Expense) must strictly adhere to these 5 app-level validation standards to prevent crashes, corrupted data, and merchant frustration during fast-paced mandi floor operations.

---

### Rule 1: Zero-Crash Parsing Guarantee
- Never use direct `.toLong()`, `.toDouble()`, or `Double(string)` without null-safety.
- Every numeric text entry must be parsed through `toLongOrNull() ?: 0L` or `MandiMathEngine.parse*`.
- Blank or whitespace strings must evaluate cleanly to `0` or default safe values without throwing `NumberFormatException`.

### Rule 2: Explicit Field-Level Validation & Visual Feedback
- All mandatory fields must highlight with `isError = true` and display Hindi supporting text (`supportingText = { Text(...) }`) when invalid.
- Error messages must explain how to fix the issue (e.g. `"मोबाइल नंबर 10 अंकों का होना चाहिए"` instead of `"अमान्य"`).
- Keyboard types must match the data type (`KeyboardType.Phone`, `KeyboardType.Number`, `KeyboardType.Decimal`).

### Rule 3: Mobile Number Validation Standard
- Mobile numbers in India must be exactly 10 digits and start with 6, 7, 8, or 9 (`^[6-9]\d{9}$`).
- Input filter must reject non-digits and cap at 10 digits (`value.filter { it.isDigit() }.take(10)`).
- If mobile is optional, an empty field is valid. If any digit is entered, all 10 digits must be completed before saving.

### Rule 4: Action Gating & Reason Banner
- Primary action buttons ("सुरक्षित करें") must be disabled (`enabled = false`) until all required fields pass validation.
- When the button is disabled, a subtle hint banner must inform the user exactly what is missing (e.g. `"कृपया किसान का नाम और कम से कम 1 फसल का बोरी व भाव दर्ज करें"`).

### Rule 5: Optional Deductions & Non-blocking Advance
- Advance payments (अग्रिम / पेशगी), extra weight, vehicle numbers, and remarks are **strictly optional**.
- Advance defaults to ₹0 and must never block standard transactions.
- If advance exceeds the transaction value, a warning is displayed and saving is blocked to prevent negative merchant payouts without explicit loan conversion.

### Rule 6: Multi-Lot / Variant Architecture
- A single arrival or trolley may contain multiple lots or crop variants (e.g. Lokwan + Sharbati).
- Each lot must validate its own bags (> 0), weight (> 0), and rate (> 0) independently.
- The parent transaction aggregates all lots into total bags, total weight, total gross, and applies uniform commission and labour charges.

### Rule 7: Soft Keyboard & Scrolling Ergonomics
- All scrollable form screens must incorporate `Modifier.imePadding()` and sufficient bottom padding (`contentPadding` / bottom spacer ≥ 80dp) so that lower fields and action buttons remain fully visible and clickable above the soft keyboard.
