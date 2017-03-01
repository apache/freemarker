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
<#macro m1 a b=a>
${a} ${b}
</#macro>
<@m1 a="1"/>
<#macro m2 a=b b="">
${a} ${b}
</#macro>
<@m2 b="2"/>
<#macro m3 d b=c[a] a=d c={"3":"4"}>
${b}
</#macro>
<@m3 d="3"/>
<#attempt>
<@m3 d="4"/>
<#recover>
m3 with d="4" Failed!
</#attempt>