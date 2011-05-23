<#ftl strict_syntax=false>
<#assign htmlEscape = "freemarker.template.utility.HtmlEscape"?new(),
         jython = "freemarker.template.utility.JythonRuntime"?new(),
         utility = "freemarker.test.templatesuite.models.TransformHashWrapper"?new()>
<html>
<head>
<title>FreeMarker: Transformation Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<@htmlEscape>
<p>${message}</p>
</@htmlEscape>

<P>Now try the Utility package:</p>
<p>${utility}</p>

<transform utility.htmlEscape>
<p>${utility}</p>
</transform>

<p>Now some nested transforms:</p>
<transform utility.compress>
<p    >This tests the compress transformation</p >
</transform>
<@utility.compress>
<transform utility.htmlEscape>
<p    >This tests the compress transformation</p >
</transform>
</@utility.compress>
<#assign html_transform = "freemarker.template.utility.HtmlEscape"?new() />
<transform html_transform><#--Using the transform via an instantiation -->
<transform utility.compress>
<p    >This tests the compress transformation</p >
</transform>
</transform>

<p>Now try method and transform interactions:</p>
<transform utility.escape( "xml" )>
<p>This isn't a valid XML string.</p>
</transform>
<transform utility.escape( "html" )>
<p>This isn't a valid HTML string.</p>
</transform>

<p>A more advanced interaction involves getting a TemplateMethodModel
to initialise a TemplateTransformModel, as follow:</p>

<transform utility.special( "This is a comment" )>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</transform>

<transform utility.special( "This is a second comment", "quote" )>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</transform>
<transform utility.special( "This is a third comment", "ampersand", "quote" )>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</transform>
<transform utility.special( "tag", utility )>
Comment: *

A test string containing quotes: "This isn't a test".
A test string containing amps: Fish & Chips.
A test string containing tags: <p>Fish &amp; Chips.</p>
</transform>

<#assign captured_output>
<compress>
<assign x=2, y=3, z = "python", adjective="cool">
<transform jython>
print 2+2
# Now we interact with the template environment somewhat.
print ${x} + ${y} 
print env['x'] # using a variable from the template
env["message"] = 'I saw the ${z}. It was ${adjective}.'

</transform>
</compress>
</#assign>

${message}

${captured_output}

</body>
</html>
