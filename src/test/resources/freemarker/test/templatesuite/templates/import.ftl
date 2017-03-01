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
--
<#-- import "/import_lib.ftl" as my -->
--

<#if mail?exists || test?exists>
  <#stop "mail or test should not exist">
</#if>

${my.mail}
<@my.test foo="bar"/>

<#assign mail="jsmith@other1.com">
${my.mail}
<@my.test foo="bar"/>

<#assign mail in my>
  jsmith@other2.com<#t>
</#assign>
${my.mail}
<@my.test foo="bar"/>

<#import "/import_lib.ftl" as my2>
${my2.mail}
<#assign mail="jsmith@other3.com" in my2>
${my.mail}

${my2.doubleUp("foobar")}