<@assertEquals expected="A" actual="a"?switch("a", "A") />
<@assertFails message="didn't match">${"b"?switch("a", "A")}</@>
<@assertEquals expected="D" actual="b"?switch("a", "A", "D") />
<@assertEquals expected="B" actual="b"?switch("a", "A", "b", "B") />
<@assertFails message="didn't match">${"c"?switch("a", "A", "b", "B")}</@>
<@assertEquals expected="D" actual="c"?switch("a", "A", "b", "B", "D") />

<#assign out = "">
<#assign fInvocationCnt = 0>
<#list 0..5 as x>
  <#assign out += x?switch(1, f("one"), 2, f("two"), 3, f("three"), f("default")) + ";">
</#list>
<@assertEquals expected="default;one;two;three;default;default;" actual=out />
<@assertEquals expected=6 actual=fInvocationCnt />

<#assign out = "">
<#list 0..5 as x>
  <#assign out += true?switch(x <= 1, "low", x == 2 || x == 3, "medium", x >= 3, "high") + ";">
</#list>
<@assertEquals expected="low;low;medium;medium;high;high;" actual=out />

<#function f x>
  <#assign fInvocationCnt++>
  <#return x>
</#function>

<@assertFails message="noSuchVar1">${1?switch(noSuchVar1, noSuchVar2)}</@>
<@assertFails message="noSuchVar2">${1?switch(1, noSuchVar2)}</@>
<@assertFails message="noSuchVar3">${noSuchVar3?switch(1, 1)}</@>

<@assertEquals expected="one" actual=1?switch(1, "one", "2", "two") />
<@assertFails messageRegexp="Can't compare.+number.+string">${2?switch(1, "one", "2", "two")}</@>
<@assertFails messageRegexp="Can't compare.+number.+string">${2?switch(1, "one", "2", "two", "default")}</@>

<#assign out><#escape x as x?switch(2 * x, "zero", 1, "one", x, x?string("0.0"))>${0} ${1} ${2}</#escape></#assign>
<@assertEquals expected="zero one 2.0" actual=out />
