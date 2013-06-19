${"freemarker.test.templatesuite.models.NewTestModel"?new("works")}
<#attempt>
${"freemarker.test.templatesuite.models.NewTestModel2"?new("works")}
<#recover>
fails
</#attempt>

<#include "subdir/new-optin.ftl">

<#include "subdir/new-optin-2.ftl">

<#include "subdir/subsub/new-optin.ftl">