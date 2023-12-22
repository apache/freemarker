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
<#setting boolean_format="c">
--
<#assign s = "abbcdbb">
${s?index_of("bb")} = 1
${s?index_of("bb", 2)} = 5
${s?index_of("")} = 0
--
${s?last_index_of("bb")} = 5
${s?last_index_of("bb", 4)} = 1
${s?last_index_of("")} = ${s?length}
--
${s?starts_with("abb")} = true
${s?starts_with("bb")} = false
${s?starts_with("")} = true
--
${s?ends_with("dbb")} = true
${s?ends_with("cbb")} = false
${s?ends_with("")} = true
--
${s?contains("abb")} = true
${s?contains("bcd")} = true
${s?contains("dbb")} = true
${s?contains("bbx")} = false
${s?contains("")} = true
--
[${s?chop_linebreak}] = [abbcdbb]
[${"qwe\n"?chop_linebreak}] = [qwe]
[${"qwe\r"?chop_linebreak}] = [qwe]
[${"qwe\r\n"?chop_linebreak}] = [qwe]
[${"qwe\r\n\r\n"?chop_linebreak}] = [qwe
]
[${"qwe\n\n"?chop_linebreak}] = [qwe
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
[${""?left_pad(5)}]
[${"a"?left_pad(5)}]
[${"ab"?left_pad(5)}]
[${"abc"?left_pad(5)}]
[${"abcd"?left_pad(5)}]
[${"abcde"?left_pad(5)}]
[${"abcdef"?left_pad(5)}]
[${"abcdefg"?left_pad(5)}]
[${"abcdefgh"?left_pad(5)}]
[${""?left_pad(5, "-")}]
[${"a"?left_pad(5, "-")}]
[${"ab"?left_pad(5, "-")}]
[${"abc"?left_pad(5, "-")}]
[${"abcd"?left_pad(5, "-")}]
[${"abcde"?left_pad(5, "-")}]
[${"abcdef"?left_pad(5, "-")}]
[${"abcdefg"?left_pad(5, "-")}]
[${"abcdefgh"?left_pad(5, "-")}]
[${""?left_pad(8, ".oO")}]
[${"a"?left_pad(8, ".oO")}]
[${"ab"?left_pad(8, ".oO")}]
[${"abc"?left_pad(8, ".oO")}]
[${"abcd"?left_pad(8, ".oO")}]
[${"abcde"?left_pad(8, ".oO")}]
[${"abcdef"?left_pad(8, ".oO")}]
[${"abcdefg"?left_pad(8, ".oO")}]
[${"abcdefgh"?left_pad(8, ".oO")}]
[${"abcdefghi"?left_pad(8, ".oO")}]
[${"abcdefghij"?left_pad(8, ".oO")}]
[${""?left_pad(0, r"/\_")}]
[${""?left_pad(1, r"/\_")}]
[${""?left_pad(2, r"/\_")}]
[${""?left_pad(3, r"/\_")}]
[${""?left_pad(4, r"/\_")}]
[${""?left_pad(5, r"/\_")}]
[${""?left_pad(6, r"/\_")}]
[${""?left_pad(7, r"/\_")}]
--
[${""?right_pad(5)}]
[${"a"?right_pad(5)}]
[${"ab"?right_pad(5)}]
[${"abc"?right_pad(5)}]
[${"abcd"?right_pad(5)}]
[${"abcde"?right_pad(5)}]
[${"abcdef"?right_pad(5)}]
[${"abcdefg"?right_pad(5)}]
[${"abcdefgh"?right_pad(5)}]
[${""?right_pad(5, "-")}]
[${"a"?right_pad(5, "-")}]
[${"ab"?right_pad(5, "-")}]
[${"abc"?right_pad(5, "-")}]
[${"abcd"?right_pad(5, "-")}]
[${"abcde"?right_pad(5, "-")}]
[${"abcdef"?right_pad(5, "-")}]
[${"abcdefg"?right_pad(5, "-")}]
[${"abcdefgh"?right_pad(5, "-")}]
[${""?right_pad(8, ".oO")}]
[${"a"?right_pad(8, ".oO")}]
[${"ab"?right_pad(8, ".oO")}]
[${"abc"?right_pad(8, ".oO")}]
[${"abcd"?right_pad(8, ".oO")}]
[${"abcde"?right_pad(8, ".oO")}]
[${"abcdef"?right_pad(8, ".oO")}]
[${"abcdefg"?right_pad(8, ".oO")}]
[${"abcdefgh"?right_pad(8, ".oO")}]
[${"abcdefghi"?right_pad(8, ".oO")}]
[${"abcdefghij"?right_pad(8, ".oO")}]
[${""?right_pad(0, r"/\_")}]
[${""?right_pad(1, r"/\_")}]
[${""?right_pad(2, r"/\_")}]
[${""?right_pad(3, r"/\_")}]
[${""?right_pad(4, r"/\_")}]
[${""?right_pad(5, r"/\_")}]
[${""?right_pad(6, r"/\_")}]
[${""?right_pad(7, r"/\_")}]