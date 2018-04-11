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
--
<#assign s = "abbcdbb">
${s?indexOf("bb")} = 1
${s?indexOf("bb", 2)} = 5
${s?indexOf("")} = 0
--
${s?lastIndexOf("bb")} = 5
${s?lastIndexOf("bb", 4)} = 1
${s?lastIndexOf("")} = ${s?length}
--
${s?startsWith("abb")?string} = true
${s?startsWith("bb")?string} = false
${s?startsWith("")?string} = true
--
${s?endsWith("dbb")?string} = true
${s?endsWith("cbb")?string} = false
${s?endsWith("")?string} = true
--
${s?contains("abb")?string} = true
${s?contains("bcd")?string} = true
${s?contains("dbb")?string} = true
${s?contains("bbx")?string} = false
${s?contains("")?string} = true
--
[${s?chopLinebreak}] = [abbcdbb]
[${"qwe\n"?chopLinebreak}] = [qwe]
[${"qwe\r"?chopLinebreak}] = [qwe]
[${"qwe\r\n"?chopLinebreak}] = [qwe]
[${"qwe\r\n\r\n"?chopLinebreak}] = [qwe
]
[${"qwe\n\n"?chopLinebreak}] = [qwe
]
--
[${s?replace("A", "-")}] = [abbcdbb]
[${s?replace("c", "-")}] = [abb-dbb]
[${s?replace("bb", "-=*")}] = [a-=*cd-=*]
--
<#assign ls = s?split("b")>
<#list ls as i>[${i}]</#list> == [a][][cd][][]
<#list "--die--maggots--!"?split("--") as i>[${i}]</#list> == [][die][maggots][!]
<#list "Die maggots!"?split("--") as i>[${i}]</#list> == [Die maggots!]
--
[${""?leftPad(5)}]
[${"a"?leftPad(5)}]
[${"ab"?leftPad(5)}]
[${"abc"?leftPad(5)}]
[${"abcd"?leftPad(5)}]
[${"abcde"?leftPad(5)}]
[${"abcdef"?leftPad(5)}]
[${"abcdefg"?leftPad(5)}]
[${"abcdefgh"?leftPad(5)}]
[${""?leftPad(5, "-")}]
[${"a"?leftPad(5, "-")}]
[${"ab"?leftPad(5, "-")}]
[${"abc"?leftPad(5, "-")}]
[${"abcd"?leftPad(5, "-")}]
[${"abcde"?leftPad(5, "-")}]
[${"abcdef"?leftPad(5, "-")}]
[${"abcdefg"?leftPad(5, "-")}]
[${"abcdefgh"?leftPad(5, "-")}]
[${""?leftPad(8, ".oO")}]
[${"a"?leftPad(8, ".oO")}]
[${"ab"?leftPad(8, ".oO")}]
[${"abc"?leftPad(8, ".oO")}]
[${"abcd"?leftPad(8, ".oO")}]
[${"abcde"?leftPad(8, ".oO")}]
[${"abcdef"?leftPad(8, ".oO")}]
[${"abcdefg"?leftPad(8, ".oO")}]
[${"abcdefgh"?leftPad(8, ".oO")}]
[${"abcdefghi"?leftPad(8, ".oO")}]
[${"abcdefghij"?leftPad(8, ".oO")}]
[${""?leftPad(0, r"/\_")}]
[${""?leftPad(1, r"/\_")}]
[${""?leftPad(2, r"/\_")}]
[${""?leftPad(3, r"/\_")}]
[${""?leftPad(4, r"/\_")}]
[${""?leftPad(5, r"/\_")}]
[${""?leftPad(6, r"/\_")}]
[${""?leftPad(7, r"/\_")}]
--
[${""?rightPad(5)}]
[${"a"?rightPad(5)}]
[${"ab"?rightPad(5)}]
[${"abc"?rightPad(5)}]
[${"abcd"?rightPad(5)}]
[${"abcde"?rightPad(5)}]
[${"abcdef"?rightPad(5)}]
[${"abcdefg"?rightPad(5)}]
[${"abcdefgh"?rightPad(5)}]
[${""?rightPad(5, "-")}]
[${"a"?rightPad(5, "-")}]
[${"ab"?rightPad(5, "-")}]
[${"abc"?rightPad(5, "-")}]
[${"abcd"?rightPad(5, "-")}]
[${"abcde"?rightPad(5, "-")}]
[${"abcdef"?rightPad(5, "-")}]
[${"abcdefg"?rightPad(5, "-")}]
[${"abcdefgh"?rightPad(5, "-")}]
[${""?rightPad(8, ".oO")}]
[${"a"?rightPad(8, ".oO")}]
[${"ab"?rightPad(8, ".oO")}]
[${"abc"?rightPad(8, ".oO")}]
[${"abcd"?rightPad(8, ".oO")}]
[${"abcde"?rightPad(8, ".oO")}]
[${"abcdef"?rightPad(8, ".oO")}]
[${"abcdefg"?rightPad(8, ".oO")}]
[${"abcdefgh"?rightPad(8, ".oO")}]
[${"abcdefghi"?rightPad(8, ".oO")}]
[${"abcdefghij"?rightPad(8, ".oO")}]
[${""?rightPad(0, r"/\_")}]
[${""?rightPad(1, r"/\_")}]
[${""?rightPad(2, r"/\_")}]
[${""?rightPad(3, r"/\_")}]
[${""?rightPad(4, r"/\_")}]
[${""?rightPad(5, r"/\_")}]
[${""?rightPad(6, r"/\_")}]
[${""?rightPad(7, r"/\_")}]