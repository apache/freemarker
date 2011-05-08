<#ftl>
<#setting locale="en_US">
<#setting number_format="0.#########">

<#assign testlist= [ 0, 1, -1, 0.5, 1.5, -0.5,
	 -1.5, 0.25, -0.25, 1.75, -1.75,
	 1.01, -1.01, 0.01, -0.01,
	 127, 128, -127, -128,
	 32767, 32768, -32767, -32768,
	 2147483647, 2147483648, -2147483647, -2147483648,
	 4294967295, 4294967296, -4294967295, -4294967296,
	 2147483647.1, 2147483648.1, -2147483647.1, -2147483648.1,
	 4294967295.1, 4294967296.1, -4294967295.1, -4294967296.1
	 2147483647.5, 2147483648.5, -2147483647.5, -2147483648.5,
	 4294967295.5, 4294967296.5, -4294967295.5, -4294967296.5
  ] />

?int:
<#list testlist as result>
    ${result}?int=${result?int}
</#list>

?double
<#list testlist as result>
    ${result}?double=${result?double}
</#list>

?long
<#list testlist as result>
    ${result}?long=${result?long}
</#list>

?long from date
    ${"2011-05-08 18:00:15 GMT"?date("yyyy-MM-dd HH:mm:ss z")?long} = 1304877615000

?float
<#list testlist as result>
    ${result}?float=${result?float}
</#list>

?byte
<#list testlist as result>
    ${result}?byte=${result?byte}
</#list>

?short
<#list testlist as result>
    ${result}?short=${result?short}
</#list>

?floor
<#list testlist as result>
    ${result}?floor=${result?floor}
</#list>

?ceiling
<#list testlist as result>
    ${result}?ceiling=${result?ceiling}
</#list>

?round
<#list testlist as result>
    ${result}?round=${result?round}
</#list>
