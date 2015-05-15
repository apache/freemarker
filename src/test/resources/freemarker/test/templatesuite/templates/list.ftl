<#assign animals = ["aardvark", "bear", "cat", "dog"]>
<#assign animal = ["aardvark"]>
<#assign nothing = []>

<@testList animals />

<@testList animal />

<@testList nothing />

<@testList arrayList />

<@testList linkedList />

<@testList set />

<@testList iterator />

<@testList emptyArrayList />

<@testList emptyLinkedList />

<@testList emptySet />

<@testList emptyIterator />

<#macro testList seq>
Size: <#attempt>${seq?size}<#recover>failed</#attempt>
Items: <#list seq as i>@${i_index} ${i}<#if i_has_next>, <#else>.</#if></#list>
</#macro>