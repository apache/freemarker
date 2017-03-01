[#ftl]
[#--
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
--]


[#macro test hash desc]
${desc} (${hash?size}):
 [#list hash?keys?sort as key]
  ${key}[#list 0 .. (16 - key?length) as x] [/#list]: [@print value=hash[key]/]
 [/#list]
[/#macro]


[#macro print value="DEBUGME"]
  [#if value?is_number || value?is_string]
    ${value}[#t]
  [#elseif value?is_boolean]
    [#if value]true[#else]false[/#if][#t]
  [#else]
    UNKNOWN[#t]
  [/#if]
[/#macro]
[@test hash=m1 desc="properties only, shadow"/]

[@test hash=m2 desc="properties only"/]

[@test hash=m3 desc="nothing, shadow"/]

[@test hash=m4 desc="nothing"/]

[@test hash=m5 desc="all, shadow"/]

[@test hash=m6 desc="all"/]

[@test hash=m7 desc="simple map mode"/]


String concatenation:
  ${s1 + s2}
  ${s3 + s4}
  ${s1 + s3}
  ${s2 + s4}
