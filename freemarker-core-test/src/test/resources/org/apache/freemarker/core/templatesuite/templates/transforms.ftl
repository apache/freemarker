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
<#assign htmlEscape = "org.apache.freemarker.core.util.HtmlEscape"?new(),
         utility = "org.apache.freemarker.core.templatesuite.models.TransformHashWrapper"?new()>
<html>
<head>
<title>FreeMarker: Transformation Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<@htmlEscape>
<p>${message}</p>
</@htmlEscape>

<P>Now try the Utility package:</p>
<p>${utility}</p>

<@utility.htmlEscape>
<p>${utility}</p>
</@>

<p>Now some nested transforms:</p>
<@utility.compress>
<p    >This tests the compress transformation</p >
</@>
<@utility.compress>
<@utility.htmlEscape>
<p    >This tests the compress transformation</p >
</@>
</@utility.compress>
<#assign html_transform = "org.apache.freemarker.core.util.HtmlEscape"?new() />
<@html_transform><#--Using the transform via an instantiation -->
<@utility.compress>
<p    >This tests the compress transformation</p >
</@>
</@>

<p>Now try method and transform interactions:</p>
<@utility.escape("xml")>
<p>This isn't a valid XML string.</p>
</@>
<@utility.escape("html")>
<p>This isn't a valid HTML string.</p>
</@>

<p>A more advanced interaction involves getting a TemplateMethodModel
to initialise a TemplateTransformModel, as follow:</p>

<@utility.special("This is a comment")>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</@>

<@utility.special("This is a second comment", "quote")>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</@>
<@utility.special("This is a third comment", "ampersand", "quote")>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</@>
<@utility.special("tag", utility)>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</@>

</body>
</html>
