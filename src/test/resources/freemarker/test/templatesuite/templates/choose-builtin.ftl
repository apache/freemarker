<#assign f1InvocationCnt = 0>
<#assign f2InvocationCnt = 0>

<@assertEquals expected="f1 1" actual=true?choose(f1(), f2()) />
<@assertEquals expected="f2 1" actual=false?choose(f1(), f2()) />
<@assertEquals expected="f1 2" actual=true?choose(f1(), f2()) />
<@assertEquals expected="f2 2" actual=false?choose(f1(), f2()) />
<@assertEquals expected=2 actual=f1InvocationCnt />
<@assertEquals expected=2 actual=f2InvocationCnt />

<#function f1>
  <#assign f1InvocationCnt += 1>
  <#return "f1 " + f1InvocationCnt>
</#function>

<#function f2>
  <#assign f2InvocationCnt += 1>
  <#return "f2 " + f2InvocationCnt>
</#function>

<#assign x = 1>
<@assertEquals expected='Y' actual=(x < 2 * x)?choose(-x < x, false)?choose('Y', 'N') />

<@assertEquals expected=1 actual=true?choose(x, noSuchVar) />
<@assertEquals expected=1 actual=false?choose(noSuchVar, x) />

<@assertFails message="noSuchVar1">${true?choose(noSuchVar1, noSuchVar2)}</@>
<@assertFails message="noSuchVar2">${false?choose(noSuchVar1, noSuchVar2)}</@>
<@assertFails message="noSuchVar3">${noSuchVar3?choose(noSuchVar1, noSuchVar2)}</@>
