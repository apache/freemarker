<#attempt>
 <#assign sequence = ["Hello, World"]>
 ${sequence[0]}
<#recover>
  We should never get here.
</#recover>
<#attempt>
 Let's try to output an undefined variable: ${undefined}
<#recover>
 Well, that did not work. Here is the error: ${truncate(.error)}
 Now we nest another attempt/recover here:
 <#attempt>
   ${sequence[1]}
 <#recover>
   Oops: ${.error[0..20]}[...]
   Remember, freeMarker sequences are zero-based! ${sequence[0]}
 </#recover>
 Now we output the current error message: ${truncate(.error)}
</#recover>
<#attempt>
  <#include "nonexistent_template">
<#recover>
  The template is not currently available
</#recover>
<#attempt>
  <#include "undefined.ftl">
<#recover>
  The included template has a problem: ${truncate(.error)}
</#attempt>
<#function truncate str>
  <#if str?length gt 110>
    <#return str[0..110]?j_string + '[...]'>
  <#else>
    <#return str>
  </#if>
</#function>
