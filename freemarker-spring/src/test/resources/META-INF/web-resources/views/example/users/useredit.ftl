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
<head>
<title>Spring MVC Form Example - User Edit Form</title>
</head>
<body>

<h1>Editing User: ${spring.eval("user.firstName + ' ' + user.lastName")}</h1>

<p>${spring.message("user.form.message", user.firstName, user.lastName, user.email)}</p>

<form method="POST" action="${spring.url('/usereditaction.do', context='/othercontext', param1='value1', param2='value2')}">
  <table border="2">
    <tbody>
      <tr>
        <th>${spring.message("user.id")!}</th>
        <td>${user.id}</td>
      </tr>
      <tr>
        <th>${spring.message("user.password")!}</th>
        <td>
          <@spring.bind "user.password"; status>
            <input type="password" name="password" value="${status.value!}" />
          </@spring.bind>
        </td>
      </tr>
      <tr>
        <th>${spring.message("user.email")!}</th>
        <td>
          <@spring.bind "user.email"; status>
            <input type="text" name="email" value="${status.value!}" />
          </@spring.bind>
        </td>
      </tr>
      <tr>
        <th>${spring.message("user.firstName")!}</th>
        <td>
          <@spring.bind "user.firstName"; status>
            <input type="text" name="firstName" value="${status.value!}" />
          </@spring.bind>
        </td>
      </tr>
      <tr>
        <th>${spring.message("user.lastName")!}</th>
        <td>
          <@spring.bind "user.lastName"; status>
            <input type="text" name="lastName" value="${status.value!}" />
          </@spring.bind>
        </td>
      </tr>
      <tr>
        <th>${spring.message("user.birthDate")!}</th>
        <td>
          <@spring.bind "user.birthDate"; status>
            ${spring.transform(status.editor, status.actualValue)!}
          </@spring.bind>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <input type="submit" value="${spring.message('user.form.submit')!'Save'}" />
          <input type="reset" value="${spring.message('user.form.submit')!'Reset'}" />
        </td>
      </tr>
    </tbody>
  </table>
</form>

</body>
</html>
