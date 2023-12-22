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
<#if exp></#if>
<#if exp>1</#if>
<#if exp>${1}2</#if>
<#if exp><#else></#if>
<#if exp>1<#else>1</#if>
<#if exp>${1}2<#else>${1}2</#if>
<#if exp><#elseif exp></#if>
<#if exp><#elseif exp>1</#if>
<#attempt><#recover></#attempt>
<#attempt>1<#recover>1</#attempt>
<#list s as i></#list>
<#list s as i>1</#list>
<#list s as i><#sep></#list>
<#list s as i>1<#sep>1</#list>
<#list s><#items as i><#sep></#items></#list>
<#list s>1<#items as i>1<#sep>1</#items>1</#list>
1
${x + y}