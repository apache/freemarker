<@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaStringList) expected="mStringArrayVsListPreference(List [a, b])" />
<@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaStringArray) expected=dow?string("mStringArrayVsListPreference(List [a, b])", "mStringArrayVsListPreference(String[] [a, b])") />
<#if dow>
  <@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaObjectArray) expected="mStringArrayVsListPreference(List [a, b])" />
<#else>
  <@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaObjectArray) expected="mStringArrayVsListPreference(List [a, b])" /></@>
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
