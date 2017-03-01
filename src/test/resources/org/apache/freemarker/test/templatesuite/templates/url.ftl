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
<#setting url_escaping_charset="utf-8">
<#assign s = 'a/báb?c/x;y=1' />
<@assertEquals expected='a%2Fb%E1b%3Fc%2Fx%3By%3D1' actual=s?url('ISO-8859-1') />
<@assertEquals expected='a%2Fb%C3%A1b%3Fc%2Fx%3By%3D1' actual=s?url />
<@assertEquals expected='a/b%E1b%3Fc/x%3By%3D1' actual=s?url_path('ISO-8859-1') />
<@assertEquals expected='a/b%C3%A1b%3Fc/x%3By%3D1' actual=s?url_path />