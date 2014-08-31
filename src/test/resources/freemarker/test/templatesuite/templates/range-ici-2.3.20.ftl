<#include 'range-common.ftl'>

<#-- Legacy quirk: right-unbounded ranges are apparently empty: -->
<@assertEquals actual=(4..)?size expected=0 />
<@assertEquals actual=join(1.., ' ') expected="" />

<#list 4.. as i>
  <#stop "Shouldn't be reached">
</#list>

<@assertFails message="missing">
	<@assertEquals actual=(4..)[0] expected=4 />
</@>
<@assertFails message="missing">
	<@assertEquals actual=(4..)[1] expected=5 />
</@>
<@assertFails message="out of bounds">
	<@assertEquals actual=(4..)[-1] expected=5 />
</@>
