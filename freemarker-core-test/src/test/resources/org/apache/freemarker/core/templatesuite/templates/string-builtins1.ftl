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
FreeMarker: Encoding string built-in tests

<#assign x = r'  dieBugsDie! * vazzZE 123456 --cdc-- --<<--@ x ${"kigyo"?upperCase}  '>
  
capFirst:   ${x?capFirst};
uncapFirst: ${x?uncapFirst};
uncapFirst: ${"Blah"?uncapFirst}
capitalize: ${x?capitalize};
html:       ${x?html};
length:     ${x?length};
lowerCase:  ${x?lowerCase};
rtf:        ${x?rtf};
trim:       ${x?trim};
trim2:      ${"foo bar"?trim}
trim3:      ${" foo bar"?trim}
trim4:      ${"foo bar "?trim}
upperCase:  ${x?upperCase};
xml:        ${x?xml};
xhtml:      ${"\"Blah's is > 1 & < 2\""?xhtml}
<@assertEquals actual="'"?html expected="&#39;" />
<@assertEquals actual="'"?xhtml expected="&#39;" />
<@assertEquals actual="'"?xml expected="&apos;" />
<#-- ?substring: -->
<@assertEquals actual="ab"?substring(0) expected="ab" />
<@assertEquals actual="ab"?substring(1) expected="b" />
<@assertEquals actual="ab"?substring(2) expected="" />
<@assertFails message="at least 0">${"ab"?substring(-1)}</@><#t>
<@assertFails message="greater than the length of the string">${"ab"?substring(3)}</@><#t>
<@assertEquals actual="ab"?substring(0, 0) expected="" />
<@assertEquals actual="ab"?substring(0, 1) expected="a" />
<@assertEquals actual="ab"?substring(0, 2) expected="ab" />
<@assertFails message="at least 0">${"ab"?substring(0, -1)}</@><#t>
<@assertFails message="greater than the length of the string">${"ab"?substring(0, 3)}</@><#t>
<@assertEquals actual="ab"?substring(1, 1) expected="" />
<@assertEquals actual="ab"?substring(1, 2) expected="b" />
<@assertFails message="at least 0">${"ab"?substring(1, -1)}</@><#t>
<@assertFails message="greater than the length of the string">${"ab"?substring(1, 3)}</@><#t>
<@assertFails message="shouldn't be greater than the end index">${"ab"?substring(1, 0)}</@><#t>

word_list:
<#global words = x?wordList>
<#list words as w>- ${w}
</#list>

<#global canufeelitbabe = x?interpret>
interpret: <@canufeelitbabe></@>
<#setting locale="es_ES">number: ${"-123.45"?number + 1.1}
${"1.5e3"?number?c}
${"0005"?number?c}
${"+0"?number?c}
${"-0"?number?c}
${"NaN"?number?isNan?c}
${("INF"?number?isInfinite && "INF"?number > 0)?c}
${("-INF"?number?isInfinite && "-INF"?number < 0)?c}
${("Infinity"?number?isInfinite && "Infinity"?number > 0)?c}
${("-Infinity"?number?isInfinite && "-Infinity"?number < 0)?c}

${"org.apache.freemarker.core.templatesuite.models.NewTestModel"?new()}
${"org.apache.freemarker.core.templatesuite.models.NewTestModel"?new(1)}
${"org.apache.freemarker.core.templatesuite.models.NewTestModel"?new("xxx")}
${"org.apache.freemarker.core.templatesuite.models.NewTestModel"?new("xxx", "yyy")}

<#assign x = "In the beginning, God created the Heavens and The Earth.">

${x?replace("the", "The Sacred, Holy", "i")} <#-- case insensitive replacement -->
${x?replace("the", "the very", "f")} <#-- replace only the first one -->
${x?replace("", "|")} <#-- replace empry string -->
${x?replace("", "|", "f")} <#-- replace first empty string -->

${x?replace("the H[a-z]+", "the sky", "r")} <#-- regexp replacement -->

<#if x?matches(".*Heav..s.*")>matches<#else>Really?</#if>

<#list x?matches("(the) ([a-z]+)", "i") as match>
  ${match}
  ${match?groups[1]} sacred ${match?groups[2]}
</#list>  

<#assign matches = x?matches("In the ([a-z]+), God created (.*)")>
${matches?groups[0]}
${matches?groups[1]}
${matches?groups[2]}

<#assign x="foo, bar;baz,     foobar">
<#list x?split("[,;] ?", "r") as word>
   ${word}
</#list>


<#assign a = "foo", b="bar", c="(a+b)?upperCase">
${c?eval}

[${"a"?jString}] = [a]
[${"a\\'x'\nb"?jString}] = [a\\'x'\nb]
[${"\x0001\x001A "?jString}] = [\u0001\u001a ]

[${"a"?jsString}] = [a]
[${"a\\'x'\nb"?jsString}] = [a\\\'x\'\nb]
[${"\x0001\x001A "?jsString}] = [\x01\x1A ]
[${"<![CDATA["?jsString}] = [\x3C![CDATA[]
[${"]]>"?jsString}] = []]\>]

[${"a"?jsonString}] = [a]
[${"a\\'x'\nb"?jsonString}] = [a\\'x'\nb]
[${"\x0001\x001A "?jsonString}] = [\u0001\u001A ]
[${"\n\r\t\f\b\""?jsonString}] = [\n\r\t\f\b\"]
[${"/"?jsonString}] = [\/]
[${"a/b"?jsonString}] = [a/b]
[${"</script>"?jsonString}] = [<\/script>]
[${"<![CDATA["?jsonString}] = [\u003C![CDATA[]
[${"]]>"?jsonString}] = []]\u003E]
