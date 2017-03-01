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
<@testList ["aardvark", "bear", "cat", "dog"] />

<@testList ["aardvark"] />

<@testList [] />

<@testList listables.list />

<@testList listables.linkedList />

<@testList listables.set />

<@testList listables.iterator />

<@testList listables.emptyList />

<@testList listables.emptyLinkedList />

<@testList listables.emptySet />

<@testList listables.emptyIterator />

<#macro testList seq>
Size: <#attempt>${seq?size}<#recover>failed</#attempt>
Items: <#list seq as i>@${i_index} ${i}<#if i_has_next>, <#else>.</#if></#list>
</#macro>