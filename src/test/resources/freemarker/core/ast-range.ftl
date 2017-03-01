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
<#assign x = 0..>

<#assign x = 0..1>
<#assign x = 0..<1>
<#assign x = 0..!1>

<#assign x = n + 1 .. m + 2>
<#assign x = n + 1 ..< m + 2>
<#assign x = n + 1 ..! m + 2>

<#assign x = n * 1 .. m * 2>

<#assign x = n?abs .. m?abs>

<#assign x = n?index_of('x') .. m?index_of('y')>

<#assign x = n..m == o..p>

<#assign x = n+1+2..m-1-2 == o+1+2..p-1-2>

<#assign x = 1+a..+2>
<#assign x = 1-a..-2>
<#assign x = 1*a..*2> 
<#assign x = a && b..c || d> 
<#assign x = a.. && b.. || d> 

${f(m.., m-1.., m+1..n-1)}

<@m 1 * m .. m - 1 m + 1 .. n - 1 m .. />
