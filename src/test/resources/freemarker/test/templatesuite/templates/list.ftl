<#assign animals = ["aardvark", "bear", "cat", "dog", "elephant"]>

<@testSeq animals />

<@testSeq arrayList />

<@testSeq linkedList />

<@testSeq set />

<@testSeq iterator />

<#macro testSeq seq>
Size: <#attempt>${seq?size}<#recover>failed</#attempt>
Items: <#list seq as i>@${i_index} ${i}<#if i_has_next>, <#else>.</#if></#list>
</#macro>