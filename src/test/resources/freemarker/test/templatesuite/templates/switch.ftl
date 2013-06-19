<html>
<head>
<title>FreeMarker: Switch-Case Test</title>
</head>
<body>

<p>Here we iterate over a list of animals.</p>
<#assign animalList = [ "aardvark", "kiwi", "gecko", "cat", "dog", "elephant",
    "squirrel", "zebra" ]>
<#assign favoriteAnimal = "kiwi">

<#foreach animal in animalList>
<p>Animal is: ${animal}.<br />
<#switch animal>
    <#case "zebra">
        This is the HTML for a large stripey animal.
    <#case "elephant">
    <#case "rhinocerous">
        This is the HTML for large animals.
        <#break>
    <#case "squirrel">
    <#case "gecko">
        This is the HTML for small animals.
        <#break>
    <#case favoriteAnimal>
        This is the HTML for the user's favorite animal.
        <#break>
    <#default>
        This is the HTML for other animals.
        <#break>
</#switch>
</p>
</#foreach>

<#-- Nesting and no-match -->
<#list [ 1, 2, 3 ] as x>
  <#switch x>
    <#case 1>
      1
      <#switch x*2>
        <#case 1>
          i1
          <#break>
        <#case 2>
          i2
          <#break>
        <#case 3>
          i3
          <#break>
        <#case 4>
          i4
          <#break>
        <#case 6>
          i6
          <#break>
      </#switch>
      <#break>     
    <#case 2>
      2
      <#switch x*2>
        <#case 1>
          i1
          <#break>
        <#case 2>
          i2
          <#break>
        <#case 3>
          i3
          <#break>
        <#case 4>
          i4
          <#-- falls through -->
        <#case 5>
          ft
          <#-- falls through -->
      </#switch>
      ft
      <#-- falls through -->     
    <#case 3>
      3
      <#switch x*2>
        <#case 1>
          i1
          <#break>
        <#case 2>
          i2
          <#break>
        <#case 3>
          i3
          <#break>
        <#case 4>
          i4
          <#break>
        <#case 6>
          i6
          <#break>
      </#switch>
      <#break>     
  </#switch>
</#list>

<#-- No match -->
[<#switch 213>
  <#case 1>sadas
</#switch>]

<#-- Fall-through -->
<#list [ 0, 1, 2, 3, 4 ] as x>
  "<#switch x><#case 1>1<#case 2>2<#case 3>3<#case 4>4</#switch>"
</#list>

<#-- Legacy parser bug: #default might not be the last, but it doesn't fall through if called directly -->
<#list [1, 2, 3, 4, 5] as x>
  "<#switch x><#case 1>1<#case 2>2<#default>default<#case 4>4<#case 5>5</#switch>"
</#list>

<#-- two #default-s are parsing error -->
<@assertFails message="can only have one default"><@"<#switch 1><#case 1><#default><#default></#switch>"?interpret /></@>

</body>
</html>
