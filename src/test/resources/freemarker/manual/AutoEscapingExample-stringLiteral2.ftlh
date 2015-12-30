<#-- Markup output value created by escaping plain text: -->
<#assign mo1 = "Foo & bar"?esc>
<#-- Markup output value created outherwise: -->
<#assign mo2 = "<p>Foo"?no_esc>

${"${mo1} baz"}
${"${mo2} baz"}