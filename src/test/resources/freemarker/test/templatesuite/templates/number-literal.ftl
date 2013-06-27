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
