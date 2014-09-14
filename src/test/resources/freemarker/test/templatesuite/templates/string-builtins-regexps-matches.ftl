<#assign input>
L16
L27
L38
L49
</#assign>

List mode:
<#assign matches = input?matches(".+") >
Size: ${matches?size}
<#list 0..<matches?size as i>[${matches[i]}]</#list>
<#list matches as m>[${m}]</#list>

Iterator mode:
<#assign matches = input?matches(".+") >
<#list matches as m>[${m}]</#list>
<#list matches as m>[${m}(${matches?join(', ')})]</#list>
<#list matches as m>[${m}]</#list>

Iterator mode changes to list mode:
<#assign matches = input?matches(".+") >
<#list matches as m>[${m}]/${matches?size}</#list>
<#list matches as m>[${m}]</#list>

Iterator mode changes to list mode 2:
<#assign matches = input?matches(".+") >
<#list matches as m>[${m}]</#list>
<#list matches as m>[${m}]/${matches?size}<#t></#list>

List mode with embedded iteration:
<#assign matches = input?matches(".+") >
<#list 0..<matches?size as i>[${matches[i]}(${matches?join(', ')})]</#list>

Entire input match:
<#assign matches = input?matches(r".*(\d)(\d)") >
<#assign firstGS = false>
<#list matches as m>
- M: ${m}
    <#if firstGS?is_boolean>
      <#assign firstGS = m?groups>
    </#if>
    <#list m?groups as g>
    - G: ${g}
    </#list>
</#list>
firstGS was: ${firstGS?join(', ')}

Entire input match 2:
<#assign match = "x12"?matches(r"x(\d)(\d)") >
Matches: ${match?c}
<#list match?groups as g>
- G: ${g}
</#list>
As list:
<#list match as m>
- M: ${m}
  <#list m?groups as g>
    - G: ${g}
  </#list>
</#list>
Groups again:
<#list match?groups as g>
- G: ${g}
</#list>

Entire input match 3:
<#assign match = "x12"?matches(r"y(\d)(\d)") >
Matches: ${match?c}
<@assertEquals expected=3 actual=match?groups?size />
<@assertEquals expected=0 actual=match?size />

Entire input match 4:
<#assign match = "x12"?matches(r"x(\d)(\d)") >
Matches: ${match?c}
<#assign gs = match?groups>
<@assertEquals expected=3 actual=gs?size />
<@assertEquals expected=1 actual=match?size />
- G: ${gs[0]}
- G: ${match?groups[1]}
- G: ${gs[2]}

Substring match nested into entire input match:
<#assign match = "x12"?matches(r"x(\d)(\d)") >
<#list match?groups as g>
- G: ${g} (<#list match as m>[${m}{${m?groups?join(', ')}}]</#list>)
</#list>

Different entire input and substring matches:
<#assign match = "123"?matches(r"(\d+?)") >
${match?groups?join(", ")}
<#list match as m>
- M: ${m} (Gs: ${m?groups?join(", ")})
</#list>

Different entire input and substring matches 2:
<#assign match = "123"?matches(r"\d+?") >
${match?groups?join(", ")}
<#list match as m>
- M: ${m} (Gs: ${m?groups?join(", ")})
</#list>