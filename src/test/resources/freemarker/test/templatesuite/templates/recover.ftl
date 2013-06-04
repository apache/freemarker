<#attempt>
 <#assign sequence = ["Hello, World"]>
 ${sequence[0]}
<#recover>
  We should never get here.
</#recover>
<#attempt>
 Let's try to output an undefined variable: ${undefinedVariable}
<#recover>
 Well, that did not work.<@assert test=.error?contains('undefinedVariable') />
 Now we nest another attempt/recover here:
 <#attempt>
   ${sequence[1]}
 <#recover>
   Oops...<@assert test=.error?contains('sequence[1]') />
   Remember, freeMarker sequences are zero-based! ${sequence[0]}
 </#recover>
 Now we check the current error message.<@assert test=.error?contains('undefinedVariable') />
</#recover>
<#attempt>
  <#include "nonexistent_template">
<#recover>
  The template is not currently available
</#recover>
<#attempt>
  <#include "undefined.ftl">
<#recover>
  The included template had a problem.<@assert test=.error?contains('undefined_variable') />
</#attempt>
