<#ftl output_format="HTML"><#-- Needed for the Eclipse plugin -->

<#-- Markup output value created by escaping plain text: -->
<#assign mo1 = "Foo & bar"?esc>
<#-- Markup output value created outherwise: -->
<#assign mo2 = "<p>Foo"?no_esc>

${"${mo1} baz"}
<#attempt>${"${mo2} baz"}<#recover>Failed</#attempt>