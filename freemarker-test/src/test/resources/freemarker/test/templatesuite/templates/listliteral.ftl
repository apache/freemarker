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
<title>FreeMarker: List Literal Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Now perform a list assignment:</p>

<#assign hash = {"temp", "Temporary"}>
<#assign mymessage = "hello">
<#assign test = [ "test1", "test23", "test45", message, mymessage]>

The list contains #{test?size} items.

<#foreach item in test>
<p>${item}</p>
</#foreach>

<p>Now update the assignment and repeat:</p>

<#assign mymessage = "world">

<#foreach item in test>
<p>${item}</p>
</#foreach>

<p>Now reassign the list and repeat:</p>

<#assign test = [ hash.temp, "test1", "test23", "test45", mymessage, "hash", hash["temp"]]>
<#assign test = [ "foo", "bar" ] + test>

<#foreach item in test[1..4]>
<p>${item}</p>
</#foreach>

<p>Silly, but necessary tests, for one and zero element lists:</p>

<#assign test = [ "Hello, world" ]>

<#foreach item in test>
<p>${item}</p>
</#foreach>

<p>Zero item test:</p>

<#assign test = []>

<#foreach item in test>
<p>${item}</p>
</#foreach>

<p>Dumb test for number literals -- these weren't working as expected:</p>

<#assign test = [] + [1, 2,3, 5, 7]>

<#foreach item in test>
<p>${item}</p>
<#if item == 5><#break></#if>
</#foreach>

</body>
</html>
