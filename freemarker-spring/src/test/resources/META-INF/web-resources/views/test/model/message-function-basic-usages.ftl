<#ftl outputFormat="HTML">
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
<html>
<body>

<#if user??>
  <div id="userId">${spring.message("user.id")!}</div>
  <div id="userEmail">${spring.message("user.email")!}</div>
  <div id="userInfoWithArgs">${spring.message("user.form.message", user.firstName, user.lastName, user.email)!}</div>
<#else>
  <div id="errorMessage">${spring.message(message=errorMessage)!}</div>
</#if>

</body>
</html>
