# MandiSamiti Revamp — Flow 05: Stock & Inventory Flow (गोदाम स्टॉक)

## 1. Flow Overview & Scope
Maintains real-time stock balances per commodity in the godown/shop.
- Purchases (S1) increment stock (grams + bags).
- Sales (S4) decrement stock.
- S10 (`StockBreakdownSheet`): Bottom sheet / modal showing current stock across all commodities with bags count, total quintals, and warning for negative/low stock.

---

## 2. Test Cases Matrix

| Case ID | Scenario | Input Data | Expected Output | Status |
| :--- | :--- | :--- | :--- | :--- |
| **TC-STK-01** | Buy Inflow | Buy 10 bags Wheat (5.00 Qtl) | Wheat stock increments by +10 bags, +500,000 grams | PENDING |
| **TC-STK-02** | Sell Outflow | Sell 4 bags Wheat (2.00 Qtl) | Wheat stock decrements by -4 bags, -200,000 grams | PENDING |
| **TC-STK-03** | Multi-commodity Sheet | Display sheet with Wheat, Mustard, Chana | Each commodity displays exact bags & quintals | PENDING |
| **TC-STK-04** | Low Stock Warning | Commodity stock < configured minimum | Yellow alert chip displayed beside commodity | PENDING |
