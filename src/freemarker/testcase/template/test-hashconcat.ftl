[#ftl]
[#assign a = {"a":1, "b":2, "c":3, "X": 4}]
[#assign b = {"d":10, "e":20, "f":30, "X": 40}]

a:
[@dump a /]

B:
[@dump b /]

a + B:
[@dump a + b /]

B + a:
[@dump b + a /]

a + a:
[@dump a + a /]

{} + a:
[@dump {} + a /]

a + {}:
[@dump a + {} /]

{} + {}:
[@dump {} + {} /]

a + b + {} + b + {} + a:
[@dump a + b + {} + b + {} + a /]


[#macro dump s]
[#list s?keys as k]
  ${k} = ${s[k]}
[/#list]
  ---
[#list s?values as v]
  ${v}
[/#list]
  ---
[/#macro]