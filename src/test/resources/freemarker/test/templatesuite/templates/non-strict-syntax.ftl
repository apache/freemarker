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
<title>FreeMarker: Identifier Test</title>
</head>
<body>
<p>A simple test follows:</p>

<p>${message}</p>

<p>A tag inside a tag...</p>

<#assign f="f">
<i<if message?exists>>this is a test<else>>no message</if></i>

<i${f}>Blah</i${f}>
<ifthisisindeedworkingthisshouldnotparseasafreemarkertag>

<<if message?exists>b>This is a test</b<else>strong>no message</strong</if>>
</body>
</html>
