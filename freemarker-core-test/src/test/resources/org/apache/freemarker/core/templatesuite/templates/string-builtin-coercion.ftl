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
Was broken 2.3.19:
<#setting numberFormat="0.#">
<@assert 1232?contains('2') />
<@assert 1232?indexOf('2') == 1 />
<@assert 1232?lastIndexOf('2') == 3 />
<@assert 1232?leftPad(6) == '  1232' /><@assert 1232?leftPad(6, '0') == '001232' />
<@assert 1232?rightPad(6) == '1232  ' /><@assert 1232?rightPad(6, '0') == '123200' />
<@assert 1232?matches('[1-3]+') />
<@assert 1232?replace('2', 'z') == '1z3z' />
<@assert 1232?replace('2', 'z', 'r') == '1z3z' />
<@assert 1232?split('2')[1] == '3' /><@assert 1232?split('2')[2] == '' />
<@assert 1232?split('2', 'r')[1] == '3' />

Was no broken in 2.3.19:
<@assert 1232?startsWith('12') />
<@assert 1232?endsWith('32') />
