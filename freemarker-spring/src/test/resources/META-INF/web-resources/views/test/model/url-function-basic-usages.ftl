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

<h2 id="usersListHeader">
  <a href="${spring.url('/users/')}">Users List</a>
</h2>

<h3 id="usersListHeaderWithSortParams">
  <a href="${spring.url('/users/', sortField='birthDate', sortDirection='descending')}">Users List</a>
</h3>

<h2 id="otherAppsUsersListHeader">
  <a href="${spring.url('/users/', context='/otherapp')}">Users List</a>
</h2>

<h3 id="otherAppsUsersListHeaderWithSortParams">
  <a href="${spring.url('/users/', context='/otherapp', sortField='birthDate', sortDirection='descending')}">Users List</a>
</h3>

<ul>
  <#list users as user>
    <li>
      <div id="user-${user.id!}">
        <a class="userIdLink" href="${spring.url('/users/{userId}/', userId=user.id)}">${user.id!}</a>
        <a class="userNameLink" href="${spring.url('/users/${user.id}/')}">${user.firstName!} ${user.lastName!}</a>
      </div>
    </li>
  </#list>
</ul>

<div id="freeMarkerManualUrl">
  <a href="${spring.url('http://freemarker.org/docs/index.html')}">Apache FreeMarker Manual</a>
</div>

</body>
</html>
