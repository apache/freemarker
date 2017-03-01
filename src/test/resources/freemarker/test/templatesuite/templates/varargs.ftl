<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<#setting number_format="#">

${m.bar()} == 0
${m.bar([])} == 0
${m.bar(11)} == 11
${m.bar(null, 11)} == 11
${m.bar(11, 22)} == 1122
${m.bar(11.6, 22.4)} == 1122
${m.bar(11, 22, 33)} == 112233
${m.bar([11, 22, 33])} == 112233

${m.bar2(11, [22, 33, 44])} == -22334411
${m.bar2(11, 22, 33)} == -223311
${m.bar2(11, 22)} == -2211
${m.bar2(11)} == -11

${m.overloaded()} == 0
${m.overloaded(11)} == -11
${m.overloaded(11, 22)} == 1122
${m.overloaded(11, 22, 33)} == -112233
${m.overloaded(11, 22, 33, 44)} == -11223344
${m.overloaded([11, 22, 33, 44, 55])} == -1122334455

${m.overloaded(11, 22)} == 1122
${m.overloaded([11, 22])} == -1122

${m.noVarArgs("string", true, 123, 1000000?number_to_date)} == string, true, 123, 1000000
