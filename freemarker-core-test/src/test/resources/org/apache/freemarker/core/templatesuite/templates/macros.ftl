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
<title>FreeMarker: Function Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Now perform function tests:</p>

<#assign urls = {"home" : "/home.html", "about" : "/about.html"}>
<#assign images = {"home" : "/images/home.png", "about" : "/image/about-us.jpeg"}>
<#assign preferences = {"showImages" : true}>
<#assign español = français><#macro français url image alt>
    <#local var = "Kilroy">
    <a href="${url}">
    <#if preferences.showImages>
        <img src="${image}" border="0" alt="${alt}">
    <#else>
        ${alt}
    </#if>
    </a>
    ${var} was here.
</#macro>

<p>Function is defined, now let's call it:</p>

   <@español url=urls.home image=images.home alt="Home" /><#t>

<p>Again, but with different parameters:</p>

<@français 
   url=urls.about 
   image=images.about 
   alt="About Us"
/>

<#if var?exists>
   Something is wrong here.
<#else>
   Good.
</#if>

<p>A recursive function call:</p>

<#macro recurse dummy{positional}, a{positional}=3>
    <#if (a > 0)>
        <@recurse dummy, a - 1 />
    </#if>
    ${a}
</#macro>

<@recurse urls />

<p>Test "catch-all" macro parameter:</p>

<#macro catch\-all foo bar...>
foo=${foo} bar=[<#list bar as k, v>${k}=${v}<#sep>, </#list>]
</#macro>
<#assign catchall = .namespace["catch-all"]>

<@catchall foo="a"/>
<@catchall foo="a" bar="b"/>
<@catchall foo="a" bar="b" baz="c"/>

<#macro fmt pattern{positional} args...>
  <#list args as k, v>
    <#local pattern = pattern?replace("{" + k + "}", v)>
  </#list>
  ${pattern}<#lt>
</#macro>

<#macro m a=1 b=2></#macro>
<@assertFails message='"c"'><@m c=3 /></@>
<@assertFails message='position'><@m 1, 2 /></@>

<@fmt "Hello {name}! Today is {today}." name="World" today="Monday" />

</body>
</html>
