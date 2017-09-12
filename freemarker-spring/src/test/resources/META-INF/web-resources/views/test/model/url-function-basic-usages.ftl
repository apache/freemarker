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
  <#assign pathInfo="/users/" />
  <a href="${spring.url(pathInfo)}">Users List</a>
</h2>

<h3 id="usersListHeaderWithSortParams">
  <#assign pathInfo="/users/" />
  <a href="${spring.url(pathInfo, sortField='birthDate', sortDirection='descending')}">Users List</a>
</h3>

<h2 id="otherAppsUsersListHeader">
  <#assign pathInfo="/users/" />
  <a href="${spring.url(pathInfo, context='/otherapp')}">Users List</a>
</h2>

<h3 id="otherAppsUsersListHeaderWithSortParams">
  <#assign pathInfo="/users/" />
  <a href="${spring.url(pathInfo, context='/otherapp', sortField='birthDate', sortDirection='descending')}">Users List</a>
</h3>

<ul>
  <#list users as user>
    <li>
      <div id="user-${user.id!}">
        <#assign pathInfo="/users/{userId}/" />
        <a class="userIdLink" href="${spring.url(pathInfo, userId=user.id?string)}">${user.id!}</a>
        <#assign pathInfo="/users/${user.id}/" />
        <a class="userNameLink" href="${spring.url(pathInfo)}">${user.firstName!} ${user.lastName!}</a>
      </div>
    </li>
  </#list>
</ul>

<div id="freeMarkerManualUrl">
  <#assign pathInfo="http://freemarker.org/docs/index.html" />
  <a href="${spring.url(pathInfo)}">Apache FreeMarker Manual</a>
</div>

</body>
</html>
