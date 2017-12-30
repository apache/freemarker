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

  <h1>Form 1</h1>
  <hr/>
  <form id="form1">
    <table>
      <tr>
        <th>E-Mail:</th>
        <td>
          <@spring.form.input 'user.email' id="customEmailId" />
        </td>
      </tr>
      <tr>
        <th>First name:</th>
        <td>
          <@spring.form.input 'user.firstName' />
        </td>
      </tr>
      <tr>
        <th>Last name:</th>
        <td>
          <@spring.form.input 'user.lastName' />
        </td>
      </tr>
    </table>
  </form>

</body>
</html>
