<#outputformat "HTML"><#assign htmlMO><p>Test</#assign></#outputformat>
<#outputformat "XML"><#assign xmlMO><p>Test</p></#assign></#outputformat>
<#outputformat "RTF"><#assign rtfMO>\par Test</#assign></#outputformat>
<#-- We assume that we have "undefined" output format here. -->
HTML: ${htmlMO}
XML:  ${xmlMO}
RTF:  ${rtfMO}