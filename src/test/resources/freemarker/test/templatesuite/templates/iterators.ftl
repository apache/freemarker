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
<html>
<head>
<title>FreeMarker: List Iterator Test</title>
</head>
<body>
<#assign list= ["one", "two", "three", "four", "five"]>
<#assign hash = {"key", list}>
<#assign  hash2 = {"value", hash}>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Now iterate over a list:</p>

<#foreach item in list>
<p>${item}</p>
</#foreach>

<p>Now iterate again:</p>

<#list list as item>
<p>${item_index}. ${item}</p>
</#list>

<p>Iterate over a list in a hash:</p>

<#list hash.key as item>
<p>${item}</p>
</#list>

<#foreach item in hash.key>
<p>${item}</p>
</#foreach>

<#foreach item in hash[ "key" ]>
<p>${item}</p>
</#foreach>

<#list hash["key"] as item>
<p>${item}</p>
</#list>

<p>Now test the list and foreach keywords...</p>

<#list hash2["value"].key as key>
<p>${key}</p>
</#list>

<#foreach az in hash2.value.key>
<p>${az}</p>
</#foreach>

</body>
</html>
