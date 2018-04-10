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

<table class="table">
  <tbody>
    <tr>
      <th>E-Mail</th>
      <td>
        <@spring.bind "user.email"; status>
          <input type="text" name="email" value="${status.value!}" />
        </@spring.bind>
      </td>
    </tr>
    <tr>
      <th>First Name</th>
      <td>
        <@spring.bind "user.firstName"; status>
          <input type="text" name="firstName" value="${status.value!}" />
        </@spring.bind>
      </td>
    </tr>
    <tr>
      <th>Last Name</th>
      <td>
        <@spring.bind "user.lastName"; status>
          <input type="text" name="lastName" value="${status.value!}" />
        </@spring.bind>
      </td>
    </tr>
  </tbody>
</table>

<div id="statusValueNotReachable"><#if status??>${status.value!}</#if></div>

</body>
</html>
