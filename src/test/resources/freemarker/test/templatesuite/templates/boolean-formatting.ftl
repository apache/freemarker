<#assign suppress>
<@assertFails message="true,false">${true}</@>
<@assertFails message="true,false">${false}</@>
<@assertFails message="true,false">${"" + true}</@>
<@assertFails message="true,false">${"" + false}</@>
<@assertFails message="true,false">${true?upper_case}</@>
<@assertFails message="true,false">${false?upper_case}</@>
<@assertEquals expected="true" actual=true?string />
<@assertEquals expected="false" actual=false?string />
<@assertEquals expected="true" actual=true?c />
<@assertEquals expected="false" actual=false?c />
<@assertEquals expected="t" actual=true?string('t', 'f') />
<@assertEquals expected="f" actual=false?string('t', 'f') />
<#setting boolean_format = 'true,false'>
<@assertFails message="true,false">${true}</@>

<#setting boolean_format = 'ja,nein'>
<@assertEquals expected="ja" actual="" + true />
<@assertEquals expected="nein" actual="" + false />
<@assertEquals expected="JA" actual=true?upper_case />
<@assertEquals expected="NEIN" actual=false?upper_case />
<@assertEquals expected="ja" actual=true?string />
<@assertEquals expected="nein" actual=false?string />
<@assertEquals expected="true" actual=true?c />
<@assertEquals expected="false" actual=false?c />
<@assertEquals expected="t" actual=true?string('t', 'f') />
<@assertEquals expected="f" actual=false?string('t', 'f') />

<#setting boolean_format = 'y,n'>
<#assign x = false>
<#assign n = 123><#assign m = { x: 'foo', n: 'bar' }><@assertEquals actual=m['n'] + m['123'] expected='foobar' />
<@assertFails exception="UnexpectedTypeException">${m[false]}</@>
<@assertFails message="can't compare">${x == 'false'}</@>
<@assertFails message="can't compare">${x != 'false'}</@>
<@assertFails message="can't convert">${booleanVsStringMethods.expectsString(x)}</@>
<@assertEquals actual=booleanVsStringMethods.expectsString(booleanAndString) expected="theStringValue" />
<@assertEquals actual=booleanVsStringMethods.expectsBoolean(x) expected=false />
<@assertEquals actual=booleanVsStringMethods.expectsBoolean(booleanAndString) expected=true />
<@assertEquals actual=booleanVsStringMethods.overloaded(x) expected="boolean false" />
<@assertEquals actual=123?upper_case expected="123" />
<@assertEquals actual=true?upper_case expected="Y" />

</#assign>
<#escape x as x?upper_case>
<#assign x = true>${x} ${true} ${true?string}
<#assign x = false>${x} ${false} ${false?string}
<#noescape><#assign x = true>${x} ${true} ${true?string}</#noescape>
</#escape>
<#assign x = false>${x} ${false} ${false?string}
<#assign x = true>${x} ${true} ${true?string}
<#assign x = false>${x} ${false} ${false?string}
${'str:' + x} ${'str:' + false}
${x?string('ja', 'nein')} ${true?string('ja', 'nein')}
${beansBoolean} ${beansBoolean?string}
${booleanAndString} ${booleanAndString?string}

<#setting boolean_format = 'y,n'>
<@assertEquals actual='true'?boolean expected=true />
<@assertEquals actual='false'?boolean expected=false />
<@assertEquals actual='y'?boolean expected=true />
<@assertEquals actual='n'?boolean expected=false />
<@assertFails message="can't convert">${'N'?boolean}</@>
<@assertFails message="can't convert">${'True'?boolean}</@>
<@assertFails message="can't convert">${0?boolean}</@>
<@assertFails message="sequence">${[]?boolean}</@>