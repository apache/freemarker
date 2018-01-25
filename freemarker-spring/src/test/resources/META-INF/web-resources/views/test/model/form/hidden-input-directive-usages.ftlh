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
        <th>Hidden1 :</th>
        <td>
          <@form.hidden 'user.email' />
        </td>
      </tr>
    </table>
  </form>

  <hr/>

  <form id="form2">
    <table>
      <tr>
        <th>Hidden 2:</th>
        <td>
          <@form.hidden 'user.email' disabled=true />
        </td>
      </tr>
    </table>
  </form>

  <hr/>

  <h2>Testing with setting showPassword to false explicitly</h2>
  <form id="form3">
    <table>
      <tr>
        <th>Password:</th>
        <td>
          <@form.password 'user.password' showPassword=false />
        </td>
      </tr>
    </table>
  </form>

</body>
</html>
