/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
   multiplier coming from generator.ftl.