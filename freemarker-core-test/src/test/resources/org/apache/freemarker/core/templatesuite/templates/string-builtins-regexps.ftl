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
${"test"?matches('test')?string} == true
${"test"?matches('test', '')?string} == true

${"TEST"?matches('test')?string} == false
${"TEST"?matches('test', 'i')?string} == true

${"test\nfoo"?matches('.*^foo')?string} == false
${"test\nfoo"?matches(r'.*\n^foo', 'm')?string} == true

${"test\nfoo"?matches('test.foo')?string} == false
${"test\nfoo"?matches('test.foo', 's')?string} == true

${"test\nFoo"?matches('.*foo', 's')?string} == false
${"test\nFoo"?matches('.*foo', 'i')?string} == false
${"test\nFoo"?matches('.*foo', 'im')?string} == false
${"test\nFoo"?matches('.*foo', 'si')?string} == true
${"test\nFoo"?matches('.*foo', 'is')?string} == true
${"test\nFoo"?matches('.*foo', 'mis')?string} == true

${"test\nFoo"?matches('.*\n^foo', 'm')?string} == false
${"test\nFoo"?matches('.*\n^foo', 'i')?string} == false
${"test\nFoo"?matches('.*\n^foo', 'im')?string} == true
${"test\nFoo"?matches('.*\n^foo', 'mi')?string} == true
${"test\nFoo"?matches('.*^foo', 'ism')?string} == true
${"test\nFoo"?matches('.*^foo', 'smi')?string} == true
<#setting booleanFormat="True,False">
<@assert false?matches('[eslaF]+') />
<@assert 'False'?matches('[eslaF]+') />

<#assign s = "Code without test coverage\nis considered to be BROKEN">

Lower 'c'-words:
<#list s?matches('c[a-z]*') as m>
- ${m}
</#list>

Any 'c'-words:
<#list s?matches('c[a-z]*', 'i') as m>
- ${m}
</#list>  

Lower line-last words:
<#list s?matches('[a-z]+$', 'm') as m>
- ${m}
</#list>  

Any line-last words:
<#list s?matches('[a-z]+$', 'mi') as m>
- ${m}
</#list>

Any last words:
<#list s?matches('[a-z]+$', 'i') as m>
- ${m}
</#list>

c-word with follower:
<#list s?matches('(c[a-z]*+).([a-z]++)', 'is') as m>
- "${m?jString}"
  Groups: <#list m?groups as g>"${g?jString}"<#if g_has_next>, </#if></#list>
</#list>

c-word with follower in the same line:
<#list s?matches('c[a-z]*+.[a-z]++', 'i') as m>
- ${m}
</#list>

Lower c-word with follower in the same line:
<#list s?matches('c[a-z]*+.[a-z]++', '') as m>
- ${m}
</#list>

<#attempt>
  Ignored but logged in 2.3: ${s?matches('broken', 'I')?string} == False
<#recover>
  Fails in 2.4
</#attempt>
<#attempt>
  Ignored but logged in 2.3: ${s?matches('broken', 'f')?string} == False
<#recover>
  Fails in 2.4
</#attempt>

${"foobar"?replace("foo", "FOO")} == FOObar
${"Foobar"?replace("foo", "FOO", "")} == Foobar
${"Foobar"?replace("foo", "FOO", "i")} == FOObar
${"FoobarfOO"?replace("foo", "FOO", "i")} == FOObarFOO
${"FoobarfOO"?replace("foo", "FOO", "if")} == FOObarfOO
${"FoobarfOO"?replace("foo", "FOO", "fi")} == FOObarfOO
${"Foobar"?replace("foo", "FOO", "r")} == Foobar
${"Foobar"?replace("foo", "FOO", "ri")} == FOObar
${"FoobarfOO"?replace("foo", "FOO", "ri")} == FOObarFOO
${"FoobarfOO"?replace("foo", "FOO", "rif")} == FOObarfOO
${"FoobarfOO"?replace("foo", "FOO", "fri")} == FOObarfOO
${"foobar"?replace("fo+", "FOO")} == foobar
${"foobar"?replace("fo+", "FOO", "")} == foobar
${"foobar"?replace("fo+", "FOO", "r")} == FOObar
${"foobarfOo"?replace("fo+", "FOO", "ri")} == FOObarFOO
${"foobarfOo"?replace("fo+", "FOO", "rif")} == FOObarfOo
${false?replace('a', 'A')} == FAlse
${false?replace('[abc]', 'A', 'r')} == FAlse

<#attempt>
  Ignored but logged in 2.3: ${"foobar"?replace("foo", "FOO", "c")}
<#recover>
  Fails in 2.4
</#attempt>

<#macro dumpList xs{positional}>[<#list xs as x>${x}<#if x_has_next>, </#if></#list>]</#macro>
<@dumpList "fooXbarxbaaz"?split("X") /> == [foo, barxbaaz]
<@dumpList "fooXbarxbaaz"?split("X", "") /> == [foo, barxbaaz]
<@dumpList "fooXbarxbaaz"?split("X", "i") /> == [foo, bar, baaz]
<@dumpList "fooXbarxbaaz"?split("X", "r") /> == [foo, barxbaaz]
<@dumpList "fooXbarxbaaz"?split("X", "ri") /> == [foo, bar, baaz]
<@dumpList "fooXXbarxxbaaz"?split("X+", "i") /> == [fooXXbarxxbaaz]
<@dumpList "fooXXbarxxbaaz"?split("X+", "ri") /> == [foo, bar, baaz]
<@dumpList false?split("[ae]", "r") /> == [F, ls]
<@dumpList false?split("e") /> == [Fals, ]