This FMPP project is used for generating the source code of some
`freemarker.ext.beans.OverloadedNumberUtil` methods based on the
content of `prices.ods` (LibreOffice spreadsheet).

Usage:
1. Edit `prices.ods`
3. If you have introduced new types in it, also update `toCsFreqSorted` and
   `toCsCostBoosts` and `toCsContCosts` in `config.fmpp`.
4. Save it into `prices.csv` (use comma as field separator)
5. Run FMPP from this directory. It will generate
   `<freemarkerProjectDir>/build/getArgumentConversionPrice.java`.
6. Copy-pase its content into `OverloadedNumberUtil.java`.
7. Ensure that the value of OverloadedNumberUtil.BIG_MANTISSA_LOSS_PRICE
   still matches the value coming from the ODS and the cellValue
   multipier coming from generator.ftl.