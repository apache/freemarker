<@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaStringList) expected="mStringArrayVsListPreference(List [a, b])" />
<@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaStringArray) expected=dowPre22?string("mStringArrayVsListPreference(List [a, b])", "mStringArrayVsListPreference(String[] [a, b])") />
<#if dow>
  <@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaObjectArray) expected="mStringArrayVsListPreference(List [a, b])" />
</#if>

<#-- Check if non-overloaded calls still work; they share some code with overloaded methods: -->
<@assertEquals actual=obj.mIntArrayNonOverloaded([1, 2, 3]) expected="mIntArrayNonOverloaded(int[] [1, 2, 3])" />
<@assertEquals actual=obj.mIntegerArrayNonOverloaded([1, 2, 3]) expected="mIntegerArrayNonOverloaded(Integer[] [1, 2, 3])" />
<@assertEquals actual=obj.mIntegerListNonOverloaded([1, 2, 3]) expected="mIntegerListNonOverloaded(List<Integer> [1, 2, 3])" />
<@assertEquals actual=obj.mStringListNonOverloaded(['a', 'b', 'c']) expected="mStringListNonOverloaded(List<String> [a, b, c])" />
<@assertEquals actual=obj.mStringListNonOverloaded(obj.javaStringList) expected="mStringListNonOverloaded(List<String> [a, b])" />
<@assertEquals actual=obj.mStringListNonOverloaded(obj.javaStringArray) expected="mStringListNonOverloaded(List<String> [a, b])" />
<@assertEquals actual=obj.mStringArrayNonOverloaded(['a', 'b', 'c']) expected="mStringArrayNonOverloaded(String[] [a, b, c])" />
<@assertEquals actual=obj.mStringArrayNonOverloaded(obj.javaStringList) expected="mStringArrayNonOverloaded(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayNonOverloaded(obj.javaStringArray) expected="mStringArrayNonOverloaded(String[] [a, b])" />
<@assertEquals actual=obj.mObjectListNonOverloaded(['a', 'b', 3]) expected="mObjectListNonOverloaded(List<Object> [a, b, 3])" />
<@assertEquals actual=obj.mObjectListNonOverloaded(obj.javaStringList) expected="mObjectListNonOverloaded(List<Object> [a, b])" />
<@assertEquals actual=obj.mObjectListNonOverloaded(obj.javaStringArray) expected="mObjectListNonOverloaded(List<Object> [a, b])" />
<@assertEquals actual=obj.mObjectArrayNonOverloaded(['a', 'b', 3]) expected="mObjectArrayNonOverloaded(Object[] [a, b, 3])" />
<@assertEquals actual=obj.mObjectArrayNonOverloaded(obj.javaStringList) expected="mObjectArrayNonOverloaded(Object[] [a, b])" />
<@assertEquals actual=obj.mObjectArrayNonOverloaded(obj.javaStringArray) expected="mObjectArrayNonOverloaded(Object[] [a, b])" />

<@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaStringArray) expected=dowPre22?string("mStringArrayVsListPreference(List [a, b])", "mStringArrayVsListPreference(String[] [a, b])") />

<@assertEquals actual=obj.mStringArrayVarargsNonOverloaded('a', 'b') expected="mStringArrayVarargsNonOverloaded(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsNonOverloaded(['a', 'b']) expected="mStringArrayVarargsNonOverloaded(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsNonOverloaded(obj.javaStringList) expected="mStringArrayVarargsNonOverloaded(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsNonOverloaded(obj.javaStringArray) expected="mStringArrayVarargsNonOverloaded(String[] [a, b])" />

<@assertEquals actual=obj.mStringArrayVarargsOverloaded1('a', 'b') expected="mStringArrayVarargsOverloaded1(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded1(['a', 'b']) expected="mStringArrayVarargsOverloaded1(List [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded1(obj.javaStringList) expected="mStringArrayVarargsOverloaded1(List [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded1(obj.javaStringArray) expected=dowPre22?string("mStringArrayVarargsOverloaded1(List [a, b])", "mStringArrayVarargsOverloaded1(String[] [a, b])") />

<@assertEquals actual=obj.mStringArrayVarargsOverloaded2('a', 'b') expected="mStringArrayVarargsOverloaded2(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded2('a') expected="mStringArrayVarargsOverloaded2(String a)" />

<@assertEquals actual=obj.mStringArrayVarargsOverloaded3(['a']) expected="mStringArrayVarargsOverloaded3(String[] [a])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded3(['a', 'b']) expected="mStringArrayVarargsOverloaded3(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded3(['a', 'b', 'c']) expected="mStringArrayVarargsOverloaded3(String[] [a, b, c])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded3('a') expected="mStringArrayVarargsOverloaded3(String[] [a])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded3('a', 'b') expected="mStringArrayVarargsOverloaded3(String a, String b)" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded3('a', 'b', 'c') expected="mStringArrayVarargsOverloaded3(String[] [a, b, c])" />

<@assertEquals actual=obj.mListOrString(['a', 'b']) expected="mListOrString(List [a, b])" />
<@assertEquals actual=obj.mListOrString('a') expected="mListOrString(String a)" />
<@assertEquals actual=obj.mListListOrString([['a'], 'b', 3]) expected="mListListOrString(List [[a], b, 3])" />
<@assertEquals actual=obj.mListListOrString('s') expected="mListListOrString(String s)" />

<#-- Because the fixed arg interpretations are ambiguous, it only considers the vararg interpretations:  -->
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4(['a', 'b', 'c']) expected="mStringArrayVarargsOverloaded4(List[] [[a, b, c]])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4('a', 'b', 'c') expected="mStringArrayVarargsOverloaded4(String[] [a, b, c])" />

<#-- Fixed arg solutions have priority: -->
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringList) expected="mStringArrayVarargsOverloaded4(List[] [[a, b]])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringArray) expected=dowPre22?string("mStringArrayVarargsOverloaded4(List[] [[a, b]])", "mStringArrayVarargsOverloaded4(String[] [a, b])") />

<#-- Choses between the vararg solutions: -->
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringList, obj.javaStringList) expected="mStringArrayVarargsOverloaded4(List[] [[a, b], [a, b]])" />

<#-- Until there's no overloading String->Character conversion work: -->
<@assertEquals actual=obj.mCharNonOverloaded('c') expected="mCharNonOverloaded(char c)" />
<@assertEquals actual=obj.mCharNonOverloaded(obj.javaString) expected="mCharNonOverloaded(char s)" />
<@assertEquals actual=obj.mCharacterNonOverloaded('c') expected="mCharacterNonOverloaded(Character c)" />
<@assertEquals actual=obj.mCharacterNonOverloaded(obj.javaString) expected="mCharacterNonOverloaded(Character s)" />

<@assertEquals actual=obj.mCharOrStringOverloaded('s', 1) expected="mCharOrStringOverloaded(String s, int 1)" />
<@assertEquals actual=obj.mCharacterOrStringOverloaded('s', 1) expected="mCharacterOrStringOverloaded(String s, int 1)" />
<@assertEquals actual=obj.mCharOrStringOverloaded2('ss') expected="mCharOrStringOverloaded2(String ss)" />
<@assertEquals actual=obj.mCharacterOrStringOverloaded2('ss') expected="mCharacterOrStringOverloaded2(String ss)" />
