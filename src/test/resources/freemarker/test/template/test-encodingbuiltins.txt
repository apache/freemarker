FreeMarker: Encoding built-in tests

<#assign x = "&<>\"'{}\\a/">
html: [${x?html}]
xml:  [${x?xml}]
xhtml: [${x?xhtml}]
rtf:  [${x?rtf}]
<#assign x = "a&a<a>a\"a'a{a}a\\">
html: [${x?html}]
xml:  [${x?xml}]
xhtml: [${x?xhtml}]
rtf:  [${x?rtf}]
<#assign x = "<<<<<">
html: [${x?html}]
xml:  [${x?xml}]
xhtml: [${x?xhtml}]
<#assign x = "{{{{{">
rtf:  [${x?rtf}]
<#assign x = "">
html: [${x?html}]
xml:  [${x?xml}]
xhtml: [${x?xhtml}]
rtf:  [${x?rtf}]
<#assign x = "a">
html: [${x?html}]
xml:  [${x?xml}]
xhtml: [${x?xhtml}]
rtf:  [${x?rtf}]
<#assign x = "&">
html: [${x?html}]
xml:  [${x?xml}]
xhtml: [${x?xhtml}]
<#assign x = "{">
rtf:  [${x?rtf}]