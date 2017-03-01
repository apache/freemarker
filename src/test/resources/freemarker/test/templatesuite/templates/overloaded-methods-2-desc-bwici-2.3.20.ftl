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

<@assertFails message="no compatible overloaded">${obj.mVarargs('a', obj.getNnS('b'), obj.getNnS('c'))}</@>
<@assertFails message="no compatible overloaded">${obj.mChar('a')}</@>
<@assertFails message="no compatible overloaded">${obj.mIntPrimVSBoxed(123?long)}</@>
<@assertEquals actual=obj.mIntPrimVSBoxed(123?short) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.mIntPrimVSBoxed(123) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.varargs4(1, 2, 3) expected='varargs4(int... xs = [1, 2, 3])' />

<@assertFails message="multiple compatible overloaded">${obj.mVarargsIgnoredTail(1, 2, 3)}</@>
<@assertFails message="multiple compatible overloaded">${obj.mVarargsIgnoredTail(1, 2, 3.5)}</@>

<@assertEquals actual=obj.mLowRankWins(1, 2, 'a') expected='mLowRankWins(int x = 1, int y = 2, Object o = a)' />

<@assertEquals actual=obj.mRareWrappings(obj.file, obj.adaptedNumber, obj.adaptedNumber, obj.adaptedNumber, obj.stringWrappedAsBoolean)
               expected='mRareWrappings(File f = file, double d1 = 123.0001, Double d2 = 123.0002, double d3 = 124.0, b = true)' />
<@assertFails message="no compatible overloaded">${obj.mRareWrappings(obj.stringWrappedAsBoolean, obj.adaptedNumber, obj.adaptedNumber, obj.adaptedNumber, obj.stringAdaptedToBoolean)}</@>
<@assertFails message="no compatible overloaded">${obj.mRareWrappings(obj.booleanWrappedAsAnotherBoolean, 0, 0, 0, obj.booleanWrappedAsAnotherBoolean)}</@>
<@assertFails message="no compatible overloaded">${obj.mRareWrappings(obj.adaptedNumber, 0, 0, 0, !obj.booleanWrappedAsAnotherBoolean)}</@>
<@assertFails message="no compatible overloaded">${obj.mRareWrappings(obj.booleanWrappedAsAnotherBoolean, 0, 0, 0, !obj.stringAdaptedToBoolean)}</@>

<@assertFails message="no compatible overloaded">${obj.mCharOrCharacterOverloaded('c')}</@>
<@assertFails message="no compatible overloaded">${obj.mCharOrCharacterOverloaded(obj.javaString)}</@>

<#include 'overloaded-methods-2-bwici-2.3.20.ftl'>
