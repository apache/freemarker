<@testList ["aardvark", "bear", "cat", "dog"] />

<@testList ["aardvark"] />

<@testList [] />

<@testList listables.list />

<@testList listables.linkedList />

<@testList listables.set />

<@testList listables.iterator />

<@testList listables.emptyList />

<@testList listables.emptyLinkedList />

<@testList listables.emptySet />

<@testList listables.emptyIterator />

<#macro testList seq>
Size: <#attempt>${seq?size}<#recover>failed</#attempt>
Items: <#list seq as i>@${i_index} ${i}<#if i_has_next>, <#else>.</#if></#list>
</#macro>