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
<#include "include2" + "-included.ftl">
<#assign s = "de">
<#include "inclu" + s + "2-included.ftl">

<#assign bTrue=true>
<#assign sY='y'>
<#include "include2-included.ftl">
<#include "include2-included.ftl" parse=true>
<#include "include2-included.ftl" parse=bTrue>
<#include "include2-included.ftl" parse='y'>
<#include "include2-included.ftl" parse=sY>

<#assign bFalse=false>
<#assign sN='n'>
<#include "include2-included.ftl" parse=false>
<#include "include2-included.ftl" parse=bFalse>
<#include "include2-included.ftl" parse='n'>
<#include "include2-included.ftl" parse=sN>

<#assign sEncoding="ISO-8859-1">
<#include "include2-included.ftl" encoding="ISO-8859-1">
<#include "include2-included.ftl" encoding=sEncoding>
<#include "include2-included-encoding.ftl" encoding="ISO-8859-1">
<#include "include2-included-encoding.ftl" encoding=sEncoding>

<#include "include2-included.ftl" ignore_missing=true>
<#include "include2-included.ftl" ignore_missing=bTrue>
<#include "include2-included.ftl" ignore_missing=false>
<#include "include2-included.ftl" ignore_missing=bFalse>

<@assertFails message="not found"><#include "missing.ftl"></@>
[<#include "missing.ftl" ignore_missing=true>]
[<#include "missing.ftl" ignore_missing=bTrue>]
