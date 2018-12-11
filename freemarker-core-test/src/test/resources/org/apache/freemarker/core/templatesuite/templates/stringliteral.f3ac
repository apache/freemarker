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
  <title>FreeMarker: String literal test</title>
  <meta http-equiv="Content-type" content="text/html; charset=UTF-8">
</head>
<body>

<p>A simple test follows:</p>

<#assign x = "Hello", y = "World">
<#assign message = "${x}, ${y}!">

${message}

<p>
[${""}] = []<br>
[${"a"}] = [a]<br>
[${"abcdef"}] = [abcdef]<br>
[${"\""}] = ["]<br>
[${"\"\"\""}] = ["""]<br>
[${"a\""}] = [a"]<br>
[${"\"a"}] = ["a]<br>
[${"a\"b"}] = [a"b]<br>
[${"a\nb"}] = [a
b]<br>
[${"'"}] = [']<br>
[${"a'a"}] = [a'a]<br>
[${"\"\'\n\r\f\b\t\l\a\g"}]<br>
[${"\xA\x0A\x00A\x000A\x0000A"}]<br>
[${"\x15Bz\x15b"}]<br>
[${"\x010Cz\x010c"}]<br>

<p>
[${''}] = []<br>
[${'a'}] = [a]<br>
[${'abcdef'}] = [abcdef]<br>
[${'"'}] = ["]<br>
[${'"""'}] = ["""]<br>
[${'a"'}] = [a"]<br>
[${'"a'}] = ["a]<br>
[${'a"b'}] = [a"b]<br>
[${'a\nb'}] = [a
b]<br>
[${'\''}] = [']<br>
[${'a\'a'}] = [a'a]<br>
[${'\"\'\n\r\f\b\t\l\a\g'}]<br>
[${'\xA\x0A\x00A\x000A\x0000A'}]<br>
[${'\x15Bz\x15b'}]<br>
[${'\x010Cz\x010c'}]<br>
</body>
</html>