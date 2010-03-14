<#ftl ns_prefixes={"D" : "http://x.com", "y" : "http://y.com"}>
<#assign r = doc.*[0]>
${r["N:t1"]?default('-')} = No NS
${r["t2"]?default('-')} = x NS
${r["y:t3"]?default('-')} = y NS
${r["./D:t4"]?default('-')} = x NS

<#assign bool = doc["true()"]>
${bool?string}

