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
1 <#assign x = 1>
2 <#assign x = 1, y = 2>
3 <#assign x = 1 in ns>
4 <#assign x = 1, y = 2 in ns>
5 <#global x = 1>
6 <#global x = 1, y = 2>
<#macro m>
  7 <#local x = 1>
  8 <#local x = 1, y = 2>
</#macro>
9 <#assign a += 1, b -= 2, c *= 3, d /= 4, e %= 5, f++, g-->
