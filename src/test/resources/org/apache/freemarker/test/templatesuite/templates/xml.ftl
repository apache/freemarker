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
<#-- test processing instructions -->
<#global PIs = doc._content._ftype("p")>
<#foreach pi in PIs>
  ${pi}
  ${pi["@target"]._text}
  ${pi["@data"]._text}
</#foreach>
${PIs?size}
<#global firstPi = PIs[0]>
${firstPi._type}
${firstPi["@customKey"]}
${doc._registerNamespace("ns", "http://www.foo.com/ns1/")}
${doc._descendant["ns:e11"]}
${doc._descendant["ns:e12"]}
<#global docRoot = doc["ns:root"]>
${docRoot["ns:e1"]}
${doc("//ns:e11")}
${docRoot["ns:e1"]["@a1"]._name}
${docRoot["ns:e1"]["@a2"]._text}
${docRoot._children._parent._name}
${docRoot._children._parent._unique._name}
<#foreach d in doc._descendant>
  ${d._name}
</#foreach>
<#foreach d in doc._descendant._ancestorOrSelf>
  ${d._name}
</#foreach>
${docRoot["ns:e2"]["ns:e12"]._text}
${docRoot["ns:e2"]["ns:e12"]._plaintext}
