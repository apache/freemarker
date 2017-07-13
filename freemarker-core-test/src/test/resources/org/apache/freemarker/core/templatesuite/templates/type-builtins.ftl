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
<#setting booleanFormat="1,0">
StNuBoMeTaMaHaHxSeCoCxEnInDiNo
<#list [
  "a", 1, false,
  testmethod, testmacro, html_escape,
  {"a":1}, [1], testcollection, testcollectionEx,
  testnode,
  bean, bean.m, bean.mOverloaded
] as x>
  ${x?isString} <#t>
  ${x?isNumber} <#t>
  ${x?isBoolean} <#t>
  ${x?isMethod} <#t>
  ${x?isMacro} <#t>
  ${x?isTransform} <#t>
  ${x?isHash} <#t>
  ${x?isHashEx} <#t>
  ${x?isSequence} <#t>
  ${x?isCollection} <#t>
  ${x?isCollectionEx} <#t>
  ${x?isEnumerable} <#t>
  ${x?isIndexable} <#t>
  ${x?isDirective} <#t>
  ${x?isNode}<#lt>
</#list>
<#macro testmacro></#macro>