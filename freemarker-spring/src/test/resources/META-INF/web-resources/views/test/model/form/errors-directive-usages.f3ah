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

  <@form.form 'user' id="form1">
    <div id="formErrors1">
      <@form.errors '*'; messages>
        <ul>
          <#list messages as message>
            <li>${message}</li>
          </#list>
        </ul>
      </@form.errors>
    </div>
    <div id="formErrors2">
      <@form.errors '*'>
        <p>There are some errors.</p>
      </@form.errors>
    </div>
    <table>
      <tr>
        <th>
          <@form.label 'firstName'>First name:</@form.label>
        </th>
        <td>
          <@form.input 'firstName' />
          <@form.errors 'firstName' cssClass="errorFirstName" />
        </td>
      </tr>
      <tr>
        <th>
          <@form.label 'lastName'>Last name:</@form.label>
        </th>
        <td>
          <@form.input 'lastName' />
          <@form.errors 'lastName' cssClass="errorLastName" />
        </td>
      </tr>
      <tr>
        <th>
          <@form.label 'email'>E-Mail:</@form.label>
        </th>
        <td>
          <@form.input 'email' />
          <@form.errors 'email' element="div" cssClass="errorEmail" />
        </td>
      </tr>
    </table>
  </@form.form>

</body>
</html>
