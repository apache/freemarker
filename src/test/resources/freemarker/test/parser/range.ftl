<#assign x = 0..>

<#assign x = 0..1>
<#assign x = 0..<1>
<#assign x = 0..!1>

<#assign x = n + 1 .. m + 2>
<#assign x = n + 1 ..< m + 2>
<#assign x = n + 1 ..! m + 2>

<#assign x = n * 1 .. m * 2>

<#assign x = n?abs .. m?abs>

<#assign x = n?index_of('x') .. m?index_of('y')>

<#assign x = n..m == o..p>

<#assign x = n+1+2..m-1-2 == o+1+2..p-1-2>

<#assign x = 1+a..+2>
<#assign x = 1-a..-2>
<#assign x = 1*a..*2> 
<#assign x = a && b..c || d> 
<#assign x = a.. && b.. || d> 

${f(m.., m-1.., m+1..n-1)}

<@m 1 * m .. m - 1 m + 1 .. n - 1 m .. />
