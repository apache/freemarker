${"freemarker.test.templatesuite.models.NewTestModel"?new("works")}
<#attempt>
${"freemarker.template.utility.ObjectConstructor"?new()("java.lang.String", "works")}
<#recover>
fails
</#attempt>