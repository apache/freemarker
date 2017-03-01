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
<title>FreeMarker: Number Literal Test</title>
</head>
<body>
<#assign hash = {"1" : "one", 
                 "12" : "twelve", 
                "2one" : "two-one", 
                "one2" : "one-two"}
         list = ["zero", 
                 "one", 
                 "two", 
                 "three", 
                 "four", 
                 "five", 
                 "six", 
                 "seven", 
                 "eight", 
                 "nine", 
                 "ten",
                 "eleven",
                 "twelve"],
          foo = "bar",
          one = "one",
          "1" = "one",
          "12" = "twelve",
          "2one" = "two-one",
          "one2" = "one-two",
          call = "freemarker.test.templatesuite.models.SimpleTestMethod"?new()
>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Now perform a number assignment:</p>

#{1.300000?double}

<#assign mynumber = 1.8, USA="en_US" />
<#assign myfloat = mynumber?float />

My number is: ${mynumber}
<#setting locale="en_US">
My float is: #{myfloat ; m6}
The int part is: ${myfloat?int}

<#assign mymessage = mynumber?string>

${mymessage + 3}

<p>Now use numbers in assignment</p>

<#assign mymessage = 1 + 5>
${mymessage}

<#assign mymessage = mymessage + 2>
#{mymessage}

<p>Try numbers in tests</p>

<#if (mymessage == 152)>
MyMessage is 152
<#else>
MyMessage is not 152, its: ${mymessage}.
</#if >

<if (mymessage > 5)>
   MyMessage is greater than five.
</if

<#switch mymessage>
	<#case 1>
		MyMessage is one
		<#break>

	<#case 15>
		MyMessage is fifteen
		<#break>
	
	<#case 152>
		MyMessage is one-five-two
		<#break>
	
	<#default>
		MyMessage is: ${mymessage}.
		<#break>
	
</#switch>

<p>Now for numbers in dynamic keys:</p>

<#assign one = 1>
<#assign two = 2>

${list[ 1 ]}
${list[ 1 + 2 ]}

<p>Numbers in hashes:</p>

${hash[ 1 + "2" ]}
${hash[ "1" + 2 ]}
${hash[ "1" + two ]}


<p>Numbers in method calls:</p>

${call( 1 )}
${call( one )}
${call( one + "2" )}
${call( one + 2 )}
${call( 1 + 2 )}

</body>
</html>
