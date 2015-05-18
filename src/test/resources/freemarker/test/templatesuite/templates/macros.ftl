<#ftl strict_syntax="false">
<html>
<head>
<title>FreeMarker: Function Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Now perform function tests:</p>

<#assign urls = {"home" : "/home.html", "about" : "/about.html"}>
<#assign images = {"home" : "/images/home.png", "about" : "/image/about-us.jpeg"}>
<#assign preferences = {"showImages" : true}>
<assign "español" = français><#macro français(url, image, alt)>
    <local var = "Kilroy">
    <a href="${url}">
    <if preferences.showImages>
        <img src="${image}" border="0" alt="${alt}">
    <else>
        ${alt}
    </if>
    </a>
    ${var} was here.
</#macro>

<p>Function is defined, now let's call it:</p>

   <call español(urls.home, images.home, "Home")><#t>

<p>Again, but with different parameters:</p>

<@français 
   url=urls.about 
   image=images.about 
   alt="About Us"
/>

<#if var?exists>
   Something is wrong here.
<else>
   Good.
</#if>

<p>A recursive function call:</p>

<macro recurse(dummy, a=3)>
    <if (a > 0)>
        <call recurse(dummy, a - 1)>
    </if>
    ${a}
</macro>

<@recurse urls />

<p>Test "catch-all" macro parameter:</p>

<#macro "catch-all" foo bar...>
foo=${foo} baz=[<#list bar?keys?sort as key>${key}=${bar[key]}<#if key_has_next>, </#if></#list>]
</#macro>
<#assign catchall = .namespace["catch-all"]>

<@catchall foo="a"/>
<@catchall foo="a" bar="b"/>
<@catchall foo="a" bar="b" baz="c"/>

<#macro fmt pattern args...>
  <#list args as arg>
    <#local pattern = pattern?replace("{" + arg_index + "}", arg)>
  </#list>
  ${pattern}<#lt>
</#macro>

<#macro m a=1 b=2>
</#macro>
<@assertFails message='"c"'><@m c=3 /></@>
<@assertFails message='3'><@m 9 8 7 /></@>

<call fmt("Hello {0}! Today is {1}.", "World", "Monday")>

</body>
</html>
