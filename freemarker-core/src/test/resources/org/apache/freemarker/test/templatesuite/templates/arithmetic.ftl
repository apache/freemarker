[#ftl]
[#--
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
--]
<html>
<head>
<title>FreeMarker: Arithmetic Test</title>
</head>
<body>
[#assign foo = 1234, bar = 23.77] 

<p>A simple test follows:</p>

<p>Perform a number assignment:</p>

[#setting locale="en_US"][#assign x = 1.2345, y=2]

#{ x+y ; m2M3}
#{ y ; m2M3}
#{ x/y ; m40M40}
#{y/x}
#{ y/x ; M4}

<P>Display a number with at least 3 digits after the decimal point</P>

#{foo ; m3}

<p>Now use numbers in assignment</p>

[#assign mynumber = foo + bar   [#-- a comment --] ]

#{mynumber}

</body>
</html>
