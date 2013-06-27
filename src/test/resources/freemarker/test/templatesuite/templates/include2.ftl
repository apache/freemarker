<html>
<head>
<title>FreeMarker: Include Instruction Test</title>
</head>
<body>

<p>A simple test follows:</p>

${message}

<if message>
	<p>Message exists!
</if>

<p>Test normal includes:</p>
<include "../bogus/included.ftl">

<p>Test unparsed includes:</p>
<include "../bogus/included.ftl" parse="n">

</body>
</html>
