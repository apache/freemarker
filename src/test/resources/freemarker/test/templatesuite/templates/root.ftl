<#ftl strict_syntax=false>
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
<title>FreeMarker: Root Lookup Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Access the same variable via the root variable (dot syntax):</p>

<p>${.data_model.message}</p>

<p>Access the same variable via the root variable (bracket syntax):</p>

<p>${.data_model["message"]}</p>

<p>Ensure that root lookups are unaffected by local variables:</p>

<macro test(message)>
  ${.data_model.message}
  ${message}
</macro>

<call test(message + " Part Deux")>

</body>
</html>
