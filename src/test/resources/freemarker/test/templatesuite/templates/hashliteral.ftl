<html>
<head>
<title>FreeMarker: Hash Literal Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Now perform a hash assignment:</p>

<#assign mymessage = "hello", foo="bar", one="1">
<#assign test = { "test1": "test23", 
                 "test45" : message, 
                  mymessage : "hello all", 
                  foo: one}>

${test.test1}
${test.test45}
${test.hello}
${test.bar}

<p>Now update the assignment and repeat:</p>

<#assign mymessage = "world">

${test.test1}
${test.test45}
${test.hello}

${test.bar}

<p>Now reassign the list and repeat:</p>

<#assign hash= {"temp" : "Temporary"}>
<#assign test = { "test1" : "test23", 
                        "test45" : message, 
                        mymessage : "hello all", 
                        foo : one, 
                        "hash" : hash[ "temp" ], 
                        "true" : hash.temp, 
                        "newhash" : hash}>

${test.test1}
${test.test45}
${test.hello?if_exists}

${test.bar}
${test.hash}
${test.true}
${test.newhash.temp}

<p>Pathological case: zero item hash:</p>

<#assign test = {}>
${test.test1?if_exists}

<p>Hash of number literals:</p>
<#assign test = {"1" : 2}>
${test["1"]}

<p>Hash concatenation:</p>
<#assign cc = { "a" : 1, "b" : 2 } + { "b" : 3, "c" : 4 }>
<#list cc?keys?sort as key>
${key} => ${cc[key]}
</#list>

<p>Empty hash concatenation:</p>
${({} + { "a" : "foo" }).a}, ${({ "a" : "bar" } + {}).a}

</body>
</html>
<@noOutput>

<#assign m = { 'a': 1, 'b', 2, 'a': 3 }>
<#if iciIntValue gte 2003021>
	<@assertEquals expected="a, b" actual=m?keys?join(', ') />
	<@assertEquals expected="3, 2" actual=m?values?join(', ') />
	<@assertEquals expected=3 actual=m['a'] />
	<@assertEquals expected=2 actual=m['b'] />
<#else>
	<@assertEquals expected="a, b, a" actual=m?keys?join(', ') />
	<@assertEquals expected="1, 2, 3" actual=m?values?join(', ') />
	<@assertEquals expected=3 actual=m['a'] />
	<@assertEquals expected=2 actual=m['b'] />
</#if>

</@noOutput>