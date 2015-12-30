<#assign mo1 = "Foo's bar {}"?esc>
HTLM: ${mo1}
XML:  <#outputformat 'XML'>${mo1}</#outputformat>
RTF:  <#outputformat 'RTF'>${mo1}</#outputformat>

<#assign mo2><p>Test</#assign>
HTML: ${mo2}
XML:  <#attempt><#outputformat 'XML'>${mo2}</#outputformat><#recover>Failed</#attempt>
RTF:  <#attempt><#outputformat 'RTF'>${mo2}</#outputformat><#recover>Failed</#attempt>
