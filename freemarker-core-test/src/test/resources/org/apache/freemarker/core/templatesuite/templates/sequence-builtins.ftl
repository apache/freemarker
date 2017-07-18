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
<@noOutput>
<#setting locale="en_US">
<#setting numberFormat="0.#########">

<#assign ls = []?sort>
<#list ls as i>
- ${i}
</#list>
<@assertEquals expected=0 actual=ls?size />
<@assertEquals expected=3 actual=set?size />
</@noOutput>
Sorting scalars:
----------------

String order:
<#assign ls = ["whale", "Barbara", "zeppelin", "aardvark", "beetroot"]?sort>
<#list ls as i>
- ${i}
</#list>

First: ${ls?first}
Last: ${ls?last}
Size ${ls?size}

Numerical order:
<#assign ls = [123?byte, 543, -324, -34?float, 0.11, 0, 111?int, 0.1?double, 1, 5]?sort>
<#list ls as i>
- ${i}
</#list>

First: ${ls?first}
Last: ${ls?last}
Size ${ls?size}

Date/time order:
<#assign x = [
        '08:05'?time('HH:mm'),
        '18:00'?time('HH:mm'),
        '06:05'?time('HH:mm'),
        '08:15'?time('HH:mm')]>
<#list x?sort as i>
- ${i?string('HH:mm')}
</#list>

Boolean order:
<#assign x = [
        true,
        false,
        false,
        true]>
<#list x?sort as i>
- ${i?string}
</#list>


Sorting hashes:
---------------

<#assign ls = [
  {"name":"whale", "weight":2000?short},
  {"name":"Barbara", "weight":53},
  {"name":"zeppelin", "weight":-200?float},
  {"name":"aardvark", "weight":30?long},
  {"name":"beetroot", "weight":0.3}
]>
Order by name:
<#assign ls = ls?sortBy("name")>
<#list ls as i>
- ${i.name}: ${i.weight}
</#list>

Order by weight:
<#assign ls = ls?sortBy("weight")>
<#list ls as i>
- ${i.name}: ${i.weight}
</#list>

Order by a.x.v:
<#assign x = [
        {"a": {"x": {"v": "qweqw", "w": "asd"}, "y": '1998-02-20'?date('yyyy-MM-dd')}},
        {"a": {"x": {"v": "aqweqw", "w": "asd"}, "y": '1999-01-20'?date('yyyy-MM-dd')}},
        {"a": {"x": {"v": "dfgdf", "w": "asd"}, "y": '1999-04-20'?date('yyyy-MM-dd')}},
        {"a": {"x": {"v": "utyu", "w": "asd"}, "y": '1999-04-19'?date('yyyy-MM-dd')}}]>
<#list x?sortBy(['a', 'x', 'v']) as i>
- ${i.a.x.v}
</#list>

Order by a.y, which is a date:
<#list x?sortBy(['a', 'y']) as i>
- ${i.a.y?string('yyyy-MM-dd')}
</#list>

Reverse:
--------

Order by weight desc:
<#assign ls = ls?reverse>
<#list ls as i>
- ${i.name}: ${i.weight}
</#list>

Order by weight desc desc:
<#assign ls = ls?reverse>
<#list ls as i>
- ${i.name}: ${i.weight}
</#list>

Order by weight desc desc desc:
<#assign ls = ls?reverse>
<#list ls as i>
- ${i.name}: ${i.weight}
</#list>

Contains:
---------

<#macro test></#macro>
<#assign x = [1, "2", true, [1,2,3], {"a":1}, test, '1992-02-21'?date('yyyy-MM-dd')]>
True:
${x?seqContains(1.0)?string}
${x?seqContains("2")?string}
${x?seqContains(true)?string}
${x?seqContains('1992-02-21'?date('yyyy-MM-dd'))?string}
${abcSet?seqContains("a")?string}
${abcSet?seqContains("b")?string}
${abcSet?seqContains("c")?string}

False:
${x?seqContains("1")?string}
${x?seqContains(2)?string}
${x?seqContains(false)?string}
${x?seqContains('1992-02-22'?date('yyyy-MM-dd'))?string}
${abcSet?seqContains("A")?string}
${abcSet?seqContains(1)?string}
${abcSet?seqContains(true)?string}

<#assign x = []>
False: ${x?seqContains(1)?string}

Index_of:
---------

<#assign x = [1, "2", true, [1,2,3], {"a":1}, test, '1992-02-21'?date('yyyy-MM-dd')]>
0 = ${x?seqIndexOf(1.0)}
1 = ${x?seqIndexOf("2")}
2 = ${x?seqIndexOf(true)}
6 = ${x?seqIndexOf('1992-02-21'?date('yyyy-MM-dd'))}
0 = ${abcSet?seqIndexOf("a")}
1 = ${abcSet?seqIndexOf("b")}
2 = ${abcSet?seqIndexOf("c")}

-1 = ${x?seqIndexOf("1")}
-1 = ${x?seqIndexOf(2)}
-1 = ${x?seqIndexOf(false)}
-1 = ${x?seqIndexOf('1992-02-22'?date('yyyy-MM-dd'))}
-1 = ${abcSet?seqIndexOf("A")}
-1 = ${abcSet?seqIndexOf(1)}
-1 = ${abcSet?seqIndexOf(true)}

<#assign x = []>
-1 = ${x?seqIndexOf(1)}

Last_index_of:
--------------

<#assign x = [1, "2", true, [1,2,3], {"a":1}, test, 1, '1992-02-21'?date('yyyy-MM-dd')]>
6 = ${x?seqLastIndexOf(1.0)}
1 = ${x?seqLastIndexOf("2")}
2 = ${x?seqLastIndexOf(true)}
7 = ${x?seqLastIndexOf('1992-02-21'?date('yyyy-MM-dd'))}
-1 = ${x?seqLastIndexOf("1")}
0 = ${abcSet?seqLastIndexOf("a")}
1 = ${abcSet?seqLastIndexOf("b")}
2 = ${abcSet?seqLastIndexOf("c")}
-1 = ${abcSet?seqLastIndexOf("A")}

Index_of and last_index_of with starting indices
------------------------------------------------

<#assign names = ["Joe", "Fred", "Joe", "Susan"]>
seq_index_of "Joe":
0 = ${names?seqIndexOf("Joe", -2)}
0 = ${names?seqIndexOf("Joe", -1)}
0 = ${names?seqIndexOf("Joe", 0)}
2 = ${names?seqIndexOf("Joe", 1)}
2 = ${names?seqIndexOf("Joe", 2)}
-1 = ${names?seqIndexOf("Joe", 3)}
-1 = ${names?seqIndexOf("Joe", 4)}
 
seq_last_index_of "Joe":
-1 = ${names?seqLastIndexOf("Joe", -2)}
-1 = ${names?seqLastIndexOf("Joe", -1)}
0 = ${names?seqLastIndexOf("Joe", 0)}
0 = ${names?seqLastIndexOf("Joe", 1)}
2 = ${names?seqLastIndexOf("Joe", 2)}
2 = ${names?seqLastIndexOf("Joe", 3)}
2 = ${names?seqLastIndexOf("Joe", 4)}
 
seq_index_of "Susan":
3 = ${names?seqIndexOf("Susan", -2)}
3 = ${names?seqIndexOf("Susan", -1)}
3 = ${names?seqIndexOf("Susan", 0)}
3 = ${names?seqIndexOf("Susan", 1)}
3 = ${names?seqIndexOf("Susan", 2)}
3 = ${names?seqIndexOf("Susan", 3)}
-1 = ${names?seqIndexOf("Susan", 4)}
 
seq_last_index_of "Susan":
-1 = ${names?seqLastIndexOf("Susan", -2)}
-1 = ${names?seqLastIndexOf("Susan", -1)}
-1 = ${names?seqLastIndexOf("Susan", 0)}
-1 = ${names?seqLastIndexOf("Susan", 1)}
-1 = ${names?seqLastIndexOf("Susan", 2)}
3 = ${names?seqLastIndexOf("Susan", 3)}
3 = ${names?seqLastIndexOf("Susan", 4)}

seq_index_of "a":
0 = ${abcSet?seqIndexOf("a", -2)}
0 = ${abcSet?seqIndexOf("a", -1)}
0 = ${abcSet?seqIndexOf("a", 0)}
-1 = ${abcSet?seqIndexOf("a", 1)}
-1 = ${abcSet?seqIndexOf("a", 2)}
-1 = ${abcSet?seqIndexOf("a", 3)}
-1 = ${abcSet?seqIndexOf("a", 4)}

seq_index_of "b":
1 = ${abcSet?seqIndexOf("b", -2)}
1 = ${abcSet?seqIndexOf("b", -1)}
1 = ${abcSet?seqIndexOf("b", 0)}
1 = ${abcSet?seqIndexOf("b", 1)}
-1 = ${abcSet?seqIndexOf("b", 2)}
-1 = ${abcSet?seqIndexOf("b", 3)}

seq_index_of "c":
2 = ${abcSet?seqIndexOf("c", -2)}
2 = ${abcSet?seqIndexOf("c", -1)}
2 = ${abcSet?seqIndexOf("c", 0)}
2 = ${abcSet?seqIndexOf("c", 1)}
2 = ${abcSet?seqIndexOf("c", 2)}
-1 = ${abcSet?seqIndexOf("c", 3)}
 
seq_last_index_of "a":
-1 = ${abcSet?seqLastIndexOf("a", -2)}
-1 = ${abcSet?seqLastIndexOf("a", -1)}
0 = ${abcSet?seqLastIndexOf("a", 0)}
0 = ${abcSet?seqLastIndexOf("a", 1)}
0 = ${abcSet?seqLastIndexOf("a", 2)}
0 = ${abcSet?seqLastIndexOf("a", 3)}
0 = ${abcSet?seqLastIndexOf("a", 4)}

seq_last_index_of "b":
-1 = ${abcSet?seqLastIndexOf("b", -2)}
-1 = ${abcSet?seqLastIndexOf("b", -1)}
-1 = ${abcSet?seqLastIndexOf("b", 0)}
1 = ${abcSet?seqLastIndexOf("b", 1)}
1 = ${abcSet?seqLastIndexOf("b", 2)}
1 = ${abcSet?seqLastIndexOf("b", 3)}

seq_last_index_of "c":
-1 = ${abcSet?seqLastIndexOf("c", -2)}
-1 = ${abcSet?seqLastIndexOf("c", -1)}
-1 = ${abcSet?seqLastIndexOf("c", 0)}
-1 = ${abcSet?seqLastIndexOf("c", 1)}
2 = ${abcSet?seqLastIndexOf("c", 2)}
2 = ${abcSet?seqLastIndexOf("c", 3)}

Sequence builtins ignoring nulls
--------------------------------

true = ${listWithNull?seqContains('c')?string}
2 = ${listWithNull?seqIndexOf('c')}
0 = ${listWithNull?seqLastIndexOf('a')}

These should throw exception, but for BC they don't:
false = ${listWithNull?seqContains(noSuchVar)?string}
-1 = ${listWithNull?seqIndexOf(noSuchVar)}
-1 = ${listWithNull?seqLastIndexOf(noSuchVar)}

Sequence built-ins failing on date-type mismatch
------------------------------------------------

<#assign x = ['1992-02-21'?date('yyyy-MM-dd'), 'foo']>
<@assertEquals actual=x?seqIndexOf('foo') expected=1 />
<@assertEquals actual=x?seqIndexOf('1992-02-21'?date('yyyy-MM-dd')) expected=0 />
<@assertFails message="dates of different types">
  0 = ${x?seqIndexOf('1992-02-21 00:00:00'?dateTime('yyyy-MM-dd HH:mm:ss'))}
</@>

Chunk
-----

<#assign ls = ['a', 'b', 'c', 'd', 'e', 'f', 'g']>
<#list ['NULL', '-'] as fill>
  <#list [1, 2, 3, 4, 5, 10] as columns>
    <@printTable ls, columns, fill />
  </#list>
</#list>
<@printTable [1, 2, 3, 4, 5, 6, 7, 8, 9], 3, 'NULL' />
<@printTable [1, 2, 3, 4, 5, 6, 7, 8, 9], 3, '-' />
<@printTable [1], 3, 'NULL' />
<@printTable [1], 3, '-' />
<@printTable [], 3, 'NULL' />
<@printTable [], 3, '-' />

<#macro printTable ls columns fill>
  columns = ${columns}, fill = ${fill}:<#lt>
  <#if fill=='NULL'>
    <#local rows = ls?chunk(columns)>
  <#else>
    <#local rows = ls?chunk(columns, fill)>
  </#if>
  Rows: ${rows?size}
  <#list rows as row>
    <#list row as i>${i} </#list>  <-- Columns: ${row?size}
  </#list>
  
</#macro>


Join
----

<#assign xs = [1, "two", "three", 4]>
- ${xs?join(", ")}
- ${[]?join(", ")}
- ${xs?join(", ", "(empty)", ".")}
- ${[]?join(", ", "(empty)", ".")}
- ${listWithNull?join(", ")}
- ${listWithNull?join(", ", "(empty)")}
- ${listWithNull?join(", ", "(empty)", ".")}
- ${listWithNullsOnly?join(", ")}
- ${listWithNullsOnly?join(", ", "(empty)")}
- ${listWithNullsOnly?join(", ", "(empty)", ".")}
- ${abcSet?join(", ", "(empty)", ".")}
- ${abcCollection?join(", ", "(empty)", ".")}
<@assertFails message="index 1">${['a', [], 'c']?join(", ", "(empty)", ".")}</@>

Misc
----

First of set 1: ${abcSet?first}
First of set 2: ${abcSetNonSeq?first}