<#setting locale="en_US">
<#assign x = 3>
${x?string["0.00"]}
${x?string("0.00")}
${'01:02:03'?time.iso?string["iso ms nz"]}
${'01:02:03'?time.iso?string("iso ms nz")}
---
${multi}
<#assign a = true>
<#assign b = false>
${a?string} ${b?string}
${a?string("yes", "no")} ${b?string("yes", "no")}
<#setting boolean_format="igen,nem"/>
${a?string} ${b?string}
<#setting number_format="0.0">
${a?string(0, 1)} ${b?string(0, 1)}
<#setting boolean_format="true,false"/>
${a?string(0, 1)?is_string?string} ${b?string(0, 1)?is_string?string}
