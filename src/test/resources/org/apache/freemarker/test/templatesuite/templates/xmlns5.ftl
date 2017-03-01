<#ftl ns_prefixes = {"D": "http://y.com", "xx" : "http://x.com"}>
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
<#assign r = doc["N:root"]>
${r["N:t1"][0]?default('-')} = No NS
${r["xx:t2"][0]?default('-')} = x NS
${r["t3"][0]?default('-')} = y NS
${r["xx:t4"][0]?default('-')} = x NS
${r["//t1"][0]?default('-')} = No NS
${r["//t2"][0]?default('-')} = -
${r["//t3"][0]?default('-')} = -
${r["//t4"][0]?default('-')} = -
