<#-- A version of "?join" that fails at null-s in the sequence: -->
<#function join seq sep=''>
  <#local r = "">
  <#list seq as i>
    <#local r = r + i>
    <#if i_has_next>
      <#local r = r + sep>
    </#if>
  </#list>
  <#return r>
</#function>

<#----------------------->
<#-- Range expressions -->

<@assertEquals actual=join(1..2, ' ') expected="1 2" />
<@assertEquals actual=join(1..1, ' ') expected="1" />
<@assertEquals actual=join(1..0, ' ') expected="1 0" />
<@assertEquals actual=join(1..-1, ' ') expected="1 0 -1" />

<@assertEquals actual=join(1..<3, ' ') expected="1 2" />
<@assertEquals actual=join(1..<2, ' ') expected="1" />
<@assertEquals actual=join(1..<1, ' ') expected="" />
<@assertEquals actual=join(1..<0, ' ') expected="1" />
<@assertEquals actual=join(1..<-1, ' ') expected="1 0" />
<@assertEquals actual=join(1..<-2, ' ') expected="1 0 -1" />

<@assertEquals actual=join(1..!3, ' ') expected="1 2" />
<@assertEquals actual=join(1..!2, ' ') expected="1" />
<@assertEquals actual=join(1..!1, ' ') expected="" />
<@assertEquals actual=join(1..!0, ' ') expected="1" />
<@assertEquals actual=join(1..!-1, ' ') expected="1 0" />
<@assertEquals actual=join(1..!-2, ' ') expected="1 0 -1" />

<@assertEquals actual='abc'[0..1] expected="ab" />


<#--------------------->
<#-- String slicing: -->

<#assign s = 'abcd'>

<@assertEquals actual=s[0..] expected="abcd" />
<@assertEquals actual=s[1..] expected="bcd" />
<@assertEquals actual=s[2..] expected="cd" />
<@assertEquals actual=s[3..] expected="d" />
<@assertEquals actual=s[4..] expected="" />

<@assertEquals actual=s[1..2] expected="bc" />
<@assertEquals actual=s[1..1] expected="b" />
<@assertEquals actual=s[0..1] expected="ab" />
<@assertEquals actual=s[0..0] expected="a" />

<@assertEquals actual=s[1..<3] expected="bc" />
<@assertEquals actual=s[1..!3] expected="bc" />
<@assertEquals actual=s[1..<2] expected="b" />
<@assertEquals actual=s[1..<0] expected="b" />
<@assertEquals actual=s[1..<1] expected="" />
<@assertEquals actual=s[0..<0] expected="" />

<@assertEquals actual=s[5..<5] expected="" />
<@assertEquals actual=s[-5..<-5] expected="" />

<#-- Legacy string backward-range bug kept for compatibility: -->
<@assertEquals actual=s[1..0] expected="" />
<@assertEquals actual=s[2..1] expected="" />
<@assertFails messageRegexp="StringIndexOutOfBounds|negative">
  <@assertEquals actual=s[0..-1] expected="" />
</@assertFails>
<@assertFails messageRegexp="StringIndexOutOfBounds|decreasing">
  <@assertEquals actual=s[3..1] expected="" />
</@assertFails>

<@assertFails message="5 is out of bounds">
  <#assign _ = s[5..] />
</@assertFails>
<@assertFails message="5 is out of bounds">
  <#assign _ = s[1..5] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-1..1] />
</@assertFails>

<#assign r = 1..2>
<@assertEquals actual=s[r] expected="bc" />

<#----------------------->
<#-- Sequence slicing: -->

<#assign s = ['a', 'b', 'c', 'd']>

<@assertEquals actual=join(s[0..]) expected="abcd" />
<@assertEquals actual=join(s[1..]) expected="bcd" />
<@assertEquals actual=join(s[2..]) expected="cd" />
<@assertEquals actual=join(s[3..]) expected="d" />
<@assertEquals actual=join(s[4..]) expected="" />

<@assertEquals actual=join(s[1..2]) expected="bc" />
<@assertEquals actual=join(s[1..1]) expected="b" />
<@assertEquals actual=join(s[0..1]) expected="ab" />
<@assertEquals actual=join(s[0..0]) expected="a" />

<@assertEquals actual=join(s[1..<3]) expected="bc" />
<@assertEquals actual=join(s[1..!3]) expected="bc" />
<@assertEquals actual=join(s[1..<2]) expected="b" />
<@assertEquals actual=join(s[1..<0]) expected="b" />
<@assertEquals actual=join(s[1..<1]) expected="" />
<@assertEquals actual=join(s[0..<0]) expected="" />

<@assertEquals actual=join(s[1..0]) expected="ba" />
<@assertEquals actual=join(s[2..1]) expected="cb" />
<@assertEquals actual=join(s[2..0]) expected="cba" />
<@assertEquals actual=join(s[5..<5]) expected="" />
<@assertEquals actual=join(s[-5..<-5]) expected="" />

<@assertFails message="5 is out of bounds">
  <#assign _ = s[5..] />
</@assertFails>
<@assertFails message="5 is out of bounds">
  <#assign _ = s[1..5] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-1..1] />
</@assertFails>
