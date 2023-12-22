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
<#function f a b c d>
  <#return "a=${a}, b=${b}, c=${c}, d=${d}">
</#function>

${f?with_args([2, 3])(1, 2)}
${f?with_args_last([2, 3])(1, 2)}

<#macro m a b others...>
  a=${a}
  b=${b}
  others:
  <#list others as k, v>
    ${k} = ${v}
  </#list>
</#macro>
<@m?with_args({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />
<@m?with_args_last({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />

<#macro m a b others...>
  <#list .args as k, v>
    ${k} = ${v}
  </#list>
</#macro>
<@m?with_args({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />
<@m?with_args_last({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />
