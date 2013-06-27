<html>
<head>
<title>FreeMarker: Include Instruction Test</title>
</head>
<body>

<p>A simple test follows:</p>

${message}

<#if message?exists>
	<p>Message exists!
</#if>

<p>Test normal includes:</p>
<#include "included.ftl">

${foo}
${nestedMessage}

<p>Test unparsed includes:</p>
<#include "included.ftl" parse=false>
<@twice>
Kilroy
</@twice>

<p>Test subdir includes:</p>
<#include "subdir/include-subdir.ftl">
</body>
</html>

