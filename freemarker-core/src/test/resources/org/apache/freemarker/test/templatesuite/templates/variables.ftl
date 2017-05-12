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
<title>FreeMarker: Variable Test</title>
</head>
<body>
[#assign list = ["one", "two", "three", "four", "five"]]
[#assign hash = {"output" : "My message.", "key" : list}]
[#assign hash2 = {"value" : hash}]
[#assign items = {"mykey" : "key", "_test", "out"}]

<p>A simple test follows:</p>

<p>${message}</p>

<p>Now get into variable nesting:</p>

<p>${hash.output}</p>
<p>${hash["output"]}</p>
<p>${hash. output}</p>
<p>${hash .output}</p>
<p>${hash 
    .output}</p>
<p>${hash 
    . output}</p>
<p>${hash ["output"]}</p>
<p>${hash
    [ "output" ]}</p>

<p>More deep nesting...</p>

<p>${hash2.value.output}</p>
<p>${hash2.value.key[0]}</p>
<p>${hash2["value"]["key"][0]}</p>


<p>Nesting inside nesting...</p>

<p>${hash2.value[ items.mykey ][ 1 ]}</p>
<p>${hash2.value[ items[ "mykey" ]][ 1 ]}</p>
<p>${hash2.value[ items[ "my" + items.mykey ]][ 1 ]}</p>
<p>${hash2.value[ items[ "my" + items["mykey"] ]][ 1 ]}</p>

<p>Test underscores...</p>

<p>${items[ "_test" ]}</p>
<p>${items._test}</p>

${"God save the queen."?word_list[1]?upper_case}

</body>
</html>
