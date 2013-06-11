FreeMarker: Encoding string built-in tests

<#assign x = r'  dieBugsDie! * vazzZE 123456 --cdc-- --<<--@ x ${"kigyo"?upper_case}  '>
  
cap_first:  ${x?cap_first}
uncap_first:${x?uncap_first}
uncap_first:${"Blah"?uncap_first}
capitalize: ${x?capitalize}
html:       ${x?html}
length:     ${x?length}
lower_case: ${x?lower_case}
rtf:        ${x?rtf}
trim:       ${x?trim}
trim2:      ${"foo bar"?trim}
trim3:      ${" foo bar"?trim}
trim4:      ${"foo bar "?trim}
upper_case: ${x?upper_case}
xml:        ${x?xml}
xhtml:      ${"\"Blah's is > 1 & < 2\""?xhtml}
<@assertEquals actual="'"?html expected="'" />
<@assertEquals actual="'"?xhtml expected="&#39;" />
<@assertEquals actual="'"?xml expected="&apos;" />

word_list:
<#global words = x?word_list>
<#foreach shortvariablenamesmakeyourcodemorereadable in words>- ${shortvariablenamesmakeyourcodemorereadable}
</#foreach>

<#global canufeelitbabe = x?interpret>
interpret: <#transform canufeelitbabe></#transform>
<#setting locale="es_ES">number: ${"-123.45"?number + 1.1}

${"freemarker.test.templatesuite.models.NewTestModel"?new()}
${"freemarker.test.templatesuite.models.NewTestModel"?new(1)}
${"freemarker.test.templatesuite.models.NewTestModel"?new("xxx")}
${"freemarker.test.templatesuite.models.NewTestModel"?new("xxx", "yyy")}

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


<#assign a = "foo", b="bar", c="(a+b)?upper_case">
${c?eval}

[${"a"?j_string}] = [a]
[${"a\\'x'\nb"?j_string}] = [a\\'x'\nb]
[${"\x1\x1A\x20"?j_string}] = [\u0001\u001a ]

[${"a"?js_string}] = [a]
[${"a\\'x'\nb"?js_string}] = [a\\\'x\'\nb]
[${"\x1\x1A\x20"?js_string}] = [\x01\x1A ]
[${"<![CDATA["?js_string}] = [\x3C![CDATA[]
[${"]]>"?js_string}] = []]\>]

[${"a"?json_string}] = [a]
[${"a\\'x'\nb"?json_string}] = [a\\'x'\nb]
[${"\x1\x1A\x20"?json_string}] = [\u0001\u001A ]
[${"\n\r\t\f\b\""?json_string}] = [\n\r\t\f\b\"]
[${"/"?json_string}] = [\/]
[${"a/b"?json_string}] = [a/b]
[${"</script>"?json_string}] = [<\/script>]
[${"<![CDATA["?json_string}] = [\u003C![CDATA[]
[${"]]>"?json_string}] = []]\u003E]
