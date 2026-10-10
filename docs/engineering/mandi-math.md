# Domain Mathematical Formulas (Exact Standards)

← [Docs index](../MANDISAMITI-MASTER-SPEC.md)

All calculations in `com.appwork.mandisamiti.domain.math.MandiMathEngine` use 64-bit integer Paisa (`Long`):

$$\text{Net Weight (Quintals)} = \text{Gross Weight} - \left(\frac{\text{Bags} \times \text{Cut per Bag (Kg)}}{100}\right)$$

$$\text{Gross Crop Value} = \frac{\text{Net Weight in Kg} \times \text{Rate per Quintal}}{100}$$

$$\text{Farmer Final Payable} = \text{Gross Crop Value} - \text{Mandi Labor} - \text{Bardana} - \text{Advance (बयाना)}$$

$$\text{Buyer Final Receivable} = \text{Gross Value} + \text{Aadhat Commission (e.g. 2.5\%)} + \text{Mandi Tax (e.g. 1.5\%)}$$
