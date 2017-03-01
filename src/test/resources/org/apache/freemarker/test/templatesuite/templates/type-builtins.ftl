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
<#setting boolean_format="1,0">
StNuBoMeTaMaHaHxSeCoCxEnInDiNo
<#foreach x in [
  "a", 1, false,
  testmethod, testmacro, html_escape,
  {"a":1}, [1], testcollection, testcollectionEx,
  testnode,
  bean, bean.m, bean.mOverloaded
]>
  ${x?is_string} <#t>
  ${x?is_number} <#t>
  ${x?is_boolean} <#t>
  ${x?is_method} <#t>
  ${x?is_macro} <#t>
  ${x?is_transform} <#t>
  ${x?is_hash} <#t>
  ${x?is_hash_ex} <#t>
  ${x?is_sequence} <#t>
  ${x?is_collection} <#t>
  ${x?is_collection_ex} <#t>
  ${x?is_enumerable} <#t>
  ${x?is_indexable} <#t>
  ${x?is_directive} <#t>
  ${x?is_node}<#lt>
</#foreach>
<#macro testmacro></#macro>