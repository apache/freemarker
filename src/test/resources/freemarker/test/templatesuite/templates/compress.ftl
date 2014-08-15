<html>
<head>
<title>FreeMarker: Compress Test</title>
</head>
<body>

<#assign utility={'standardCompress': "freemarker.template.utility.StandardCompress"?new()}>
<p>A simple test follows:</p>

<p>${message}</p>

<#compress>

  <p>This is the same message,  using the &quot;compress&quot; tag:</p>


<p>${message}</p>
</#compress>

<@utility.standardCompress buffer_size=8>

  <p>This is the same message,  using the &quot;StandardCompress&quot; transform model:</p>


<p>${message}</p>
</@>

<@utility.standardCompress single_line=true>

<p>This 
   multi-line message 
     should 
be compressed 
   to a single line.</p></@>

<p>An example where the first character is not whitespace but the second character is:</p>
<p><#compress>x y</#compress></p>

<p>The end.</p>
</body>
</html>
