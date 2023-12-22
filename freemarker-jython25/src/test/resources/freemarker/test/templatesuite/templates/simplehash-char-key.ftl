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
<@assertEquals expected="string" actual=mStringC.c />
<@assertEquals expected=1 actual=mStringC?keys?size />
<@assertEquals expected="null" actual=mStringC.d!'null' />
<@assertEquals expected=1 actual=mStringC?keys?size />

<@assertEquals expected="null" actual=mStringCNull.c!'null' />
<@assertEquals expected=1 actual=mStringCNull?keys?size />
<@assertEquals expected="null" actual=mStringCNull.d!'null' />
<@assertEquals expected=1 actual=mStringCNull?keys?size />

<@assertEquals expected="char" actual=mCharC.c />
<@assertEquals expected=1 actual=mCharC?keys?size />
<@assertEquals expected="null" actual=mCharC.d!'null' />
<@assertEquals expected=1 actual=mCharC?keys?size />

<@assertEquals expected="null" actual=mCharCNull.c!'null' />
<@assertEquals expected=1 actual=mCharCNull?keys?size />
<@assertEquals expected="null" actual=mCharCNull.d!'null' />
<@assertEquals expected=1 actual=mCharCNull?keys?size />

<@assertEquals expected="char" actual=mMixed.c />
<@assertEquals expected="string" actual=mMixed.s />
<@assertEquals expected="string2" actual=mMixed.s2 />
<@assertEquals expected="null" actual=mMixed.s2n!'null' />
<@assertEquals expected="null" actual=mMixed.wrong!'null' />
<@assertEquals expected=4 actual=mMixed?keys?size />
