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
<#assign x = 1>

- <#if x == 1>good</#if>
- <#if x == 1></#if>good
- <#if x == 1>goo${missing!'d'}</#if>
- <#if x == 0>wrong</#if>good

- <#if x == 1>good<#else>wrong</#if>
- <#if x == 0>wrong<#else>good</#if>

- <#if x == 1>good<#elseif x == 2>wrong<#else>wrong</#if>
- <#if x == 1>good<#elseif x == 1>wrong<#else>wrong</#if>
- <#if x == 0>wrong<#elseif x == 1>good<#else>wrong</#if>
- <#if x == 0>wrong<#elseif x == 2>wrong<#else>good</#if>

- <#if x == 1>good<#elseif x == 1>wrong</#if>
- <#if x == 0>wrong<#elseif x == 1>good</#if>
- <#if x == 0>wrong<#elseif x == 2>wrong</#if>good
- <#if x == 0>wrong<#elseif x == 1><#else>wrong</#if>good

<#-- Same with pre-calculable results, just in case later the dead code will be optimized out: -->
- <#if 1 == 1>good</#if>
- <#if 1 == 0>wrong</#if>good
- <#if 1 == 1>goo${missing!'d'}</#if>
- <#if 1 == 0>wrong</#if>good

- <#if 1 == 1>good<#else>wrong</#if>
- <#if 1 == 0>wrong<#else>good</#if>

- <#if 1 == 1>good<#elseif 1 == 2>wrong<#else>wrong</#if>
- <#if 1 == 1>good<#elseif 1 == 1>wrong<#else>wrong</#if>
- <#if 1 == 0>wrong<#elseif 1 == 1>good<#else>wrong</#if>
- <#if 1 == 0>wrong<#elseif 1 == 2>wrong<#else>good</#if>

- <#if 1 == 1>good<#elseif 1 == 1>wrong</#if>
- <#if 1 == 0>wrong<#elseif 1 == 1>good</#if>
- <#if 1 == 0>wrong<#elseif 1 == 2>wrong</#if>good
- <#if 1 == 0>wrong<#elseif 1 == 1><#else>wrong</#if>good

<#-- Varying branch choice of the same AST nodes: -->
<#list [1, 2, 3, 4] as x>
- <#if x == 1>1</#if>
- <#if x == 2>2</#if>
- <#if x == 3>3</#if>
- <#if x == 1>is 1<#else>isn't 1</#if>
- <#if x == 2>is 2<#else>isn't 2</#if>
- <#if x == 3>is 3<#else>isn't 3</#if>
- Finally, it's: <#if x == 1>1<#elseif x == 2>2<#elseif x == 3>3<#else>4</#if>
</#list>

<#-- nested -->
<#list [1, 2, 3] as x><#list [1, 2, 3] as y>
  <#assign y = x * x>
  <#if x == 1>
    1:
    <#if (x > y)>
      > ${y}
    <#elseif x == y>
      == ${y}
    <#else>
      <= ${y}
    </#if>
  <#elseif x == 2>
    2:
    <#if (x > y)>
      > ${y}
    <#elseif x == y>
      == ${y}
    <#else>
      <= ${y}
    </#if>
  <#else>
    3:
    <#if (x > y)>
      > ${y}
    <#elseif x == y>
      == ${y}
    <#else>
      <= ${y}
    </#if>
    <#if x == 3 && y == 3>
      End
    </#if>
  </#if>
</#list></#list>

<#-- parsing errors -->
<@assertFails message="<#elseif"><@"<#if t><#else><#elseif t2></#if>"?interpret /></@>
<@assertFails message="<#else>"><@"<#if t><#else><#else></#if>"?interpret /></@>
<@assertFails message="<#else>"><@"<#else></#else>"?interpret /></@>
<@assertFails message="<#elseif"><@"<#elseif t></#elseif>"?interpret /></@>
