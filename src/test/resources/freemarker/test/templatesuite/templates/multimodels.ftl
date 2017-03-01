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
<title>FreeMarker: Test of Multiple Model implementations</title>
</head>
<body>

<p>Let's begin with a simple model:</p>
<p>${message}</p>

<p>Cool, now get into the first model. This implements a scalar, list, and
hash as a single class. Let's try some tests...</p>

<p>${data}</p>

<p>Now as a list...</p>

<#foreach item in data>${item}<br />
</#foreach>

<p>Index into a list...</p>
<p>${data[ 1 ]}</p>
<p>List size is: ${data.size}</p>
<p>List size is: ${data["size"]}</p>

<p>Now, again, as a hash. First using dot notation, then using [] notation:</p>

<p>${data.selftest}</p>
<p>${data["selftest"]}</p>

<p>Now for the tricky stuff... use a model to index into another model...</p>
<p>${test}</p>
<p>${data[ test ]}</p>
<p>${self}</p>
<p>${data[ self + "test" ]}</p>

<p>Same thing, this time a List index...</p>
<p>${zero}</p>
<p>${data[ zero ]}</p>
<p>${data[ zero + 1 ]}</p>

<p>Now, do the same recursively...</p>
<p>${data}</p>
<p>${data.model2}</p>
<p>${data.model2( "test" )}</p>
<p>${data.model2( data, data.selftest, message )}</p>

<p>Does this really not work?</p>
<p>${data[ 10 ]}</p>
<p>${data[ 10 ].selftest}</p>
<p>${data[ 10 ].message}</p>

<p>(Again, with Hashes)</p>
<p>${data.nesting1.nested}</p>
<p>${data.nesting1.nested.selftest}</p>

<p>${data["nesting1"].nested}</p>
<p>${data["nesting1"].nested["selftest"]}</p>
<p>${data["nesting1"]["nested"]["selftest"]}</p>

<p>As I suspected! (Manual on Expressions needs updating.)</p>

<p>Second test on list size</p>
<p>${data.one.size}</p>
<p>${data.one["size"]}</p>
</body>
</html>
