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
<@assertEquals expected="" actual=""+false />
<@assertEquals expected="true" actual=""+true />
<@assertEquals expected="false" actual=false?string />  <#-- In 2.1 bool?string was error, now it does what 2.3 does -->
<@assertEquals expected="n" actual=false?string('y', 'n') />
<@assertEquals expected="false" actual=""+beanFalse />
<@assertEquals expected="true" actual=""+beanTrue />
<@assertEquals expected="false" actual=beanFalse?string />
<@assertEquals expected="n" actual=beanFalse?string('y', 'n') />
