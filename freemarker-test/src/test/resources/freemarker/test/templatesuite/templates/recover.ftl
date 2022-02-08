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
