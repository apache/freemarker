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
<#assign t = JspTaglibs["http://freemarker.org/test/taglibs/test"]>

<!-- Test repeated execution -->
<@t.testtag repeatCount=3 throwException=false
>Blah
</@>

<!-- Test 0-time execution -->
<@t.testtag repeatCount=0 throwException=false
>Blah
</@>

<!-- Test abrupt execution -->
<@t.testtag repeatCount=0 throwException=true
>Blah
</@>

<!-- Test nested execution -->
<@t.testtag repeatCount=2 throwException=false
>Outer Blah
<@t.testtag repeatCount=2 throwException=false
>Inner Blah
</@>
</@>

<!-- Test nested execution with intermittent non-JSP transform -->
<@t.testtag repeatCount=2 throwException=false>
Outer Blah
<@compress>
<@t.testtag repeatCount=2 throwException=false>
Inner Blah
</@>
</@>
</@>

<@t.simpletag bodyLoopCount=2 name="simpletag1">
foo
<@t.simpletag bodyLoopCount=3 name="simpletag2">
bar
</@>
</@>
