<#ftl output_format="HTML"><#-- Needed for the Eclipse plugin -->
<#assign markupOutput1="<b>Test</b>"?no_esc>
<#assign markupOutput2="Foo & bar"?esc>

As expected:
${markupOutput1}
${markupOutput2}

Double escaping:
${markupOutput1?markup_string}
${markupOutput2?markup_string}