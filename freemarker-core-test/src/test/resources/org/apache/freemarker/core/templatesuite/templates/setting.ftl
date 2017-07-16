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
<#setting locale='de_DE'>
<@assertEquals expected='de_DE' actual=.locale />
<@assertEquals expected='de' actual=.lang />
<@assertEquals expected='java.util.Locale "de_DE"' actual=javaObjectInfo.info(.localeObject) />

<#setting numberFormat="'f'#">
<@assertEquals expected='f1' actual=1?string />

<#setting booleanFormat="t,f">
<@assertEquals expected='t' actual=true?string />

<#setting dateFormat="'df'">
<@assertEquals expected='df' actual=.now?date?string />

<#setting timeFormat="'tf'">
<@assertEquals expected='tf' actual=.now?time?string />

<#setting dateTimeFormat="'dtf'">
<@assertEquals expected='dtf' actual=.now?string />

<#setting timeZone='GMT+00'>
<#assign t1='2000'?datetime('yyyy')>
<#setting timeZone='GMT+01'>
<#assign t2='2000'?datetime('yyyy')>
<@assertEquals expected=1000*60*60 actual=t1?long-t2?long />

<#setting sqlDateAndTimeTimeZone='GMT+01'>

<#setting urlEscapingCharset='ISO-8859-1'>
<@assertEquals expected='%E1' actual='á'?url />
<#setting urlEscapingCharset='UTF-8'>
<@assertEquals expected='%C3%A1' actual='á'?url />

<#setting outputEncoding="ISO-8859-2">
<@assertEquals expected="ISO-8859-2" actual=.outputEncoding />
