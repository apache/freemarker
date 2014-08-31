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
<@assertEquals actual=join(-1..-1, ' ') expected="-1" />
<@assertEquals actual=join(-1..1, ' ') expected="-1 0 1" />

<@assertEquals actual=join(1..<3, ' ') expected="1 2" />
<@assertEquals actual=join(1..<2, ' ') expected="1" />
<@assertEquals actual=join(1..<1, ' ') expected="" />
<@assertEquals actual=join(1..<0, ' ') expected="1" />
<@assertEquals actual=join(1..<-1, ' ') expected="1 0" />
<@assertEquals actual=join(1..<-2, ' ') expected="1 0 -1" />
<@assertEquals actual=join(-1..<0, ' ') expected="-1" />
<@assertEquals actual=join(-1..<2, ' ') expected="-1 0 1" />

<@assertEquals actual=join(1..!3, ' ') expected="1 2" />
<@assertEquals actual=join(1..!2, ' ') expected="1" />
<@assertEquals actual=join(1..!1, ' ') expected="" />
<@assertEquals actual=join(1..!0, ' ') expected="1" />
<@assertEquals actual=join(1..!-1, ' ') expected="1 0" />
<@assertEquals actual=join(1..!-2, ' ') expected="1 0 -1" />
<@assertEquals actual=join(-1..!0, ' ') expected="-1" />
<@assertEquals actual=join(-1..!2, ' ') expected="-1 0 1" />

<@assertEquals actual=join(1..*2, ' ') expected="1 2" />
<@assertEquals actual=join(1..*1, ' ') expected="1" />
<@assertEquals actual=join(1..*0, ' ') expected="" />
<@assertEquals actual=join(1..*-1, ' ') expected="1" />
<@assertEquals actual=join(1..*-2, ' ') expected="1 0" />
<@assertEquals actual=join(1..*-3, ' ') expected="1 0 -1" />
<@assertEquals actual=join(-1..*1, ' ') expected="-1" />
<@assertEquals actual=join(-1..*3, ' ') expected="-1 0 1" />

<@assertEquals actual=1 expected=(0..0)?size />
<@assertEquals actual=1 expected=(1..1)?size />
<@assertEquals actual=1 expected=(2..2)?size />
<@assertEquals actual=2 expected=(0..1)?size />
<@assertEquals actual=2 expected=(1..2)?size />
<@assertEquals actual=2 expected=(2..3)?size />
<@assertEquals actual=3 expected=(2..4)?size />
<@assertEquals actual=2 expected=(1..0)?size />
<@assertEquals actual=2 expected=(2..1)?size />
<@assertEquals actual=2 expected=(3..2)?size />
<@assertEquals actual=3 expected=(4..2)?size />

<@assertEquals actual=0 expected=(0..<0)?size />
<@assertEquals actual=0 expected=(1..<1)?size />
<@assertEquals actual=0 expected=(2..<2)?size />
<@assertEquals actual=1 expected=(0..<1)?size />
<@assertEquals actual=1 expected=(1..<2)?size />
<@assertEquals actual=1 expected=(2..<3)?size />
<@assertEquals actual=2 expected=(2..<4)?size />
<@assertEquals actual=1 expected=(1..<0)?size />
<@assertEquals actual=1 expected=(2..<1)?size />
<@assertEquals actual=1 expected=(3..<2)?size />
<@assertEquals actual=2 expected=(4..<2)?size />

<@assertEquals actual=0 expected=(0..*0)?size />
<@assertEquals actual=0 expected=(1..*0)?size />
<@assertEquals actual=0 expected=(2..*0)?size />
<@assertEquals actual=1 expected=(0..*1)?size />
<@assertEquals actual=1 expected=(1..*1)?size />
<@assertEquals actual=1 expected=(2..*1)?size />
<@assertEquals actual=2 expected=(2..*2)?size />
<@assertEquals actual=1 expected=(0..*-1)?size />
<@assertEquals actual=1 expected=(1..*-1)?size />
<@assertEquals actual=1 expected=(2..*-1)?size />
<@assertEquals actual=2 expected=(0..*-2)?size />
<@assertEquals actual=2 expected=(1..*-2)?size />
<@assertEquals actual=2 expected=(2..*-2)?size />


<#--------------------->
<#-- String slicing: -->

<#assign s = 'abcd'>

<@assertEquals actual=s[0..] expected="abcd" />
<@assertEquals actual=s[1..] expected="bcd" />
<@assertEquals actual=s[2..] expected="cd" />
<@assertEquals actual=s[3..] expected="d" />
<@assertEquals actual=s[4..] expected="" />
<@assertFails message="5 is out of bounds">
  <#assign _ = s[5..] />
</@assertFails>
<@assertFails message="6 is out of bounds">
  <#assign _ = s[6..] />
</@assertFails>

<@assertEquals actual=s[1..2] expected="bc" />
<@assertEquals actual=s[1..1] expected="b" />
<@assertEquals actual=s[0..1] expected="ab" />
<@assertEquals actual=s[0..0] expected="a" />
<@assertFails message="4 is out of bounds">
  <#assign _ = s[1..4] />
</@assertFails>
<@assertFails message="5 is out of bounds">
  <#assign _ = s[1..5] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-1..1] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-2..1] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[0..-1] />
</@assertFails>

<@assertEquals actual=s[1..<3] expected="bc" />
<@assertEquals actual=s[1..!3] expected="bc" />
<@assertEquals actual=s[1..<2] expected="b" />
<@assertEquals actual=s[1..<0] expected="b" />
<@assertEquals actual=s[1..<1] expected="" />
<@assertEquals actual=s[0..<0] expected="" />
<@assertEquals actual=s[5..<5] expected="" />
<@assertEquals actual=s[6..<6] expected="" />
<@assertEquals actual=s[-5..<-5] expected="" />
<@assertFails message="negative">
  <#assign _ = s[-5..<1] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[2..<-4] />
</@assertFails>
<@assertFails message="decreasing">
  <#assign _ = s[2..<0] />
</@assertFails>

<@assertEquals actual=s[1..*-1] expected="b" />
<@assertEquals actual=s[1..*0] expected="" />
<@assertEquals actual=s[1..*1] expected="b" />
<@assertEquals actual=s[1..*2] expected="bc" />
<@assertEquals actual=s[1..*3] expected="bcd" />
<@assertEquals actual=s[1..*4] expected="bcd" />
<@assertEquals actual=s[1..*5] expected="bcd" />
<@assertEquals actual=s[4..*1] expected="" />
<@assertEquals actual=s[5..*0] expected="" />
<@assertEquals actual=s[6..*0] expected="" />
<@assertEquals actual=s[-5..*0] expected="" />
<@assertEquals actual=s[0..*0] expected="" />
<@assertEquals actual=s[0..*-1] expected="a" />
<@assertEquals actual=s[0..*-2] expected="a" />
<@assertEquals actual=s[0..*-3] expected="a" />
<@assertFails message="5 is out of bounds">
  <#assign _ = s[5..*1] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-1..*1] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-2..*1] />
</@assertFails>
<@assertFails message="decreasing">
  <#assign _ = s[1..*-2] />
</@assertFails>
<@assertFails message="decreasing">
  <#assign _ = s[1..*-3] />
</@assertFails>
<@assertFails message="4 is out of bounds">
  <#assign _ = s[4..*-1] />
</@assertFails>

<#-- Legacy string backward-range bug kept for compatibility: -->
<@assertEquals actual=s[1..0] expected="" />
<@assertEquals actual=s[2..1] expected="" />
<@assertFails message="negative">
  <@assertEquals actual=s[0..-1] expected="" />
</@assertFails>
<@assertFails message="decreasing">
  <@assertEquals actual=s[3..1] expected="" />
</@assertFails>
<#-- But it isn't emulated for operators introduced after 2.3.20: -->
<@assertFails message="decreasing">
  <@assertEquals actual=s[3..<1] expected="" />
</@assertFails>
<@assertFails message="decreasing">
  <@assertEquals actual=s[3..*-2] expected="" />
</@assertFails>

<#assign r = 1..2>
<@assertEquals actual=s[r] expected="bc" />
<#assign r = 2..1>
<@assertEquals actual=s[r] expected="" />
<#assign r = 1..<2>
<@assertEquals actual=s[r] expected="b" />
<#assign r = 2..<4>
<@assertEquals actual=s[r] expected="cd" />
<#assign r = 2..>
<@assertEquals actual=s[r] expected="cd" />
<#assign r = 1..*2>
<@assertEquals actual=s[r] expected="bc" />

<#----------------------->
<#-- Sequence slicing: -->

<#assign s = ['a', 'b', 'c', 'd']>

<@assertEquals actual=join(s[0..]) expected="abcd" />
<@assertEquals actual=join(s[1..]) expected="bcd" />
<@assertEquals actual=join(s[2..]) expected="cd" />
<@assertEquals actual=join(s[3..]) expected="d" />
<@assertEquals actual=join(s[4..]) expected="" />
<@assertFails message="5 is out of bounds">
  <#assign _ = s[5..] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-1..] />
</@assertFails>

<@assertEquals actual=join(s[1..2]) expected="bc" />
<@assertEquals actual=join(s[1..1]) expected="b" />
<@assertEquals actual=join(s[0..1]) expected="ab" />
<@assertEquals actual=join(s[0..0]) expected="a" />
<@assertFails message="5 is out of bounds">
  <#assign _ = s[1..5] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-1..0] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[0..-1] />
</@assertFails>

<@assertEquals actual=join(s[1..<3]) expected="bc" />
<@assertEquals actual=join(s[1..!3]) expected="bc" />
<@assertEquals actual=join(s[1..<2]) expected="b" />
<@assertEquals actual=join(s[1..<0]) expected="b" />
<@assertEquals actual=join(s[1..<1]) expected="" />
<@assertEquals actual=join(s[0..<0]) expected="" />

<@assertEquals actual=join(s[1..0]) expected="ba" />
<@assertEquals actual=join(s[2..1]) expected="cb" />
<@assertEquals actual=join(s[2..0]) expected="cba" />
<@assertEquals actual=join(s[2..<0]) expected="cb" />
<@assertEquals actual=join(s[1..<0]) expected="b" />
<@assertEquals actual=join(s[0..<0]) expected="" />
<@assertEquals actual=join(s[3..<1]) expected="dc" />
<@assertEquals actual=join(s[2..<1]) expected="c" />
<@assertEquals actual=join(s[1..<1]) expected="" />
<@assertEquals actual=join(s[0..<1]) expected="a" />
<@assertEquals actual=join(s[0..<0]) expected="" />
<@assertEquals actual=join(s[5..<5]) expected="" />
<@assertEquals actual=join(s[-5..<-5]) expected="" />

<@assertEquals actual=join(s[0..*-4]) expected="a" />
<@assertEquals actual=join(s[1..*-4]) expected="ba" />
<@assertEquals actual=join(s[1..*-3]) expected="ba" />
<@assertEquals actual=join(s[1..*-2]) expected="ba" />
<@assertEquals actual=join(s[1..*-1]) expected="b" />
<@assertEquals actual=join(s[1..*0]) expected="" />
<@assertEquals actual=join(s[1..*1]) expected="b" />
<@assertEquals actual=join(s[1..*2]) expected="bc" />
<@assertEquals actual=join(s[1..*3]) expected="bcd" />
<@assertEquals actual=join(s[1..*4]) expected="bcd" />
<@assertEquals actual=join(s[1..*5]) expected="bcd" />
<@assertEquals actual=join(s[0..*3]) expected="abc" />
<@assertEquals actual=join(s[2..*3]) expected="cd" />
<@assertEquals actual=join(s[3..*3]) expected="d" />
<@assertEquals actual=join(s[4..*3]) expected="" />
<@assertFails message="5 is out of bounds">
  <#assign _ = s[5..*3] />
</@assertFails>
<@assertFails message="negative">
  <#assign _ = s[-1..*2] />
</@assertFails>

<#assign r = 1..2>
<@assertEquals actual=join(s[r]) expected="bc" />
<#assign r = 2..0>
<@assertEquals actual=join(s[r]) expected="cba" />
<#assign r = 1..<2>
<@assertEquals actual=join(s[r]) expected="b" />
<#assign r = 2..<0>
<@assertEquals actual=join(s[r]) expected="cb" />
<#assign r = 2..>
<@assertEquals actual=join(s[r]) expected="cd" />
<#assign r = 1..*2>
<@assertEquals actual=join(s[r]) expected="bc" />
<#assign r = 1..*-9>
<@assertEquals actual=join(s[r]) expected="ba" />
