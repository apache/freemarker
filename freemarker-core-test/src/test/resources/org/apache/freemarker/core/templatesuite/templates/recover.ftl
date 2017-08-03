<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<#attempt>
 <#assign sequence = ["Hello, World"]>
 ${sequence[0]}
<#recover>
  We should never get here.
</#attempt>
<#attempt>
 Let's try to output an undefined variable: ${undefinedVariable}
<#recover>
 Well, that did not work.<@assert .error?contains('undefinedVariable') />
 Now we nest another attempt/recover here:
 <#attempt>
   ${sequence[1]}
 <#recover>
   Oops...<@assert .error?contains('sequence[1]') />
   Remember, freeMarker sequences are zero-based! ${sequence[0]}
 </#attempt>
 Now we check the current error message.<@assert .error?contains('undefinedVariable') />
</#attempt>
<#attempt>
  <#include "nonexistent_template">
<#recover>
  The template is not currently available
</#attempt>
<#attempt>
  <#include "undefined.ftl">
<#recover>
  The included template had a problem.<@assert .error?contains('undefined_variable') />
</#attempt>
