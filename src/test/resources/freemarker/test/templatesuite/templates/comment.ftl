<#ftl strict_syntax=false>
<html>
<head>
<title>FreeMarker: Comment Test</title>
</head>
<body>
<#--

A simple test follows:

${message}

A more rigorous test, showing that we're not faking it:

${message@#$%&}

--><#-- > --><#-- -> --><#-- -- --><#-- -- > --><comment> > </comment><comment> </comment </comment>
<if message?exists>
	<p>Message exists!
	<comment>
		...and even generates output!
	</comment>
	</p>
</if>

a <#-- < --> b
a <#-- </comment> - -- --> b

${1 + 2 + [#-- c --] <#-- c --> <!-- c --> 3}
${<!-- > -> -- #> #] --> 7}
${<#-- glitch... --] 8}
</body>
</html>
