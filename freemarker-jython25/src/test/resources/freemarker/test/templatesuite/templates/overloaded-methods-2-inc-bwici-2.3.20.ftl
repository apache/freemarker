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
<#-- Note that the point of 2.3.20 tests is to check if bugs fixed in 2.3.21 are still emulated in pre-2.3.21 mode -->

<@assertEquals actual=obj.mVarargs('a', obj.getNnS('b'), obj.getNnS('c')) expected='mVarargs(String... a1 = abc)' />
<@assertFails message="multiple compatible overload">${obj.mChar('a')}</@>
<@assertFails message="multiple compatible overload">${obj.mIntPrimVSBoxed(123?long)}</@>
<@assertFails message="multiple compatible overload">${obj.mIntPrimVSBoxed(123?short)}</@>
<@assertFails message="multiple compatible overload">${obj.mIntPrimVSBoxed(123)}</@>
<@assertFails message="multiple compatible overload">${obj.varargs4(1, 2, 3)}</@>

<@assertEquals actual=obj.mVarargsIgnoredTail(1, 2, 3) expected='mVarargsIgnoredTail(int... is = [1, 2, 3])' />
<@assertEquals actual=obj.mVarargsIgnoredTail(1, 2, 3.5) expected='mVarargsIgnoredTail(int... is = [1, 2, 3])' />

<@assertEquals actual=obj.mLowRankWins(1, 2, 'a') expected='mLowRankWins(Integer x = 1, Integer y = 2, String s = a)' />

<@assertEquals actual=obj.mRareWrappings(obj.file, obj.adaptedNumber, obj.adaptedNumber, obj.adaptedNumber, obj.stringWrappedAsBoolean)
               expected='mRareWrappings(File f = file, double d1 = 123.0001, Double d2 = 123.0002, double d3 = 123.0002, b = true)' />
<@assertEquals actual=obj.mRareWrappings(obj.stringWrappedAsBoolean, obj.adaptedNumber, obj.adaptedNumber, obj.adaptedNumber, obj.stringAdaptedToBoolean)
               expected='mRareWrappings(String s = yes, double d1 = 123.0001, Double d2 = 123.0002, double d3 = 123.0002, b = false)' />
<@assertEquals actual=obj.mRareWrappings(obj.booleanWrappedAsAnotherBoolean, 0, 0, 0, obj.booleanWrappedAsAnotherBoolean)
               expected='mRareWrappings(Object o = true, double d1 = 0.0, Double d2 = 0.0, double d3 = 0.0, b = false)' />
<@assertEquals actual=obj.mRareWrappings(obj.adaptedNumber, 0, 0, 0, !obj.booleanWrappedAsAnotherBoolean)
               expected='mRareWrappings(Object o = 124, double d1 = 0.0, Double d2 = 0.0, double d3 = 0.0, b = true)' />
<@assertEquals actual=obj.mRareWrappings(obj.booleanWrappedAsAnotherBoolean, 0, 0, 0, !obj.stringAdaptedToBoolean)
               expected='mRareWrappings(Object o = true, double d1 = 0.0, Double d2 = 0.0, double d3 = 0.0, b = true)' />

<@assertFails message="multiple compatible overloaded">${obj.mCharOrCharacterOverloaded('c')}</@>
<@assertFails message="multiple compatible overloaded">${obj.mCharOrCharacterOverloaded(obj.javaString)}</@>

<#include 'overloaded-methods-2-bwici-2.3.20.ftl'>
