[#ftl]

[#assign m = {"a": 1, "b": 2}]
[#assign ls = [1, 2]]
[#assign ambigousMsg = "Multiple compatible overloaded"]
[#assign noMatchingMsg = "No compatible overloaded"]

[@assertEquals actual=obj.oneArg(null) expected='oneArg<Object>(null)' /]
[@assertEquals actual=obj.oneArg("s") expected='oneArg<String>("s")' /]
[@assertFails message=ambigousMsg]${obj.oneArg(true)}[/@] (should be oneArg<Boolean>)
[@assertEquals actual=obj.oneArg(ls) expected='oneArg<List>([1, 2])' /]
[@assertEquals actual=obj.oneArg(m) expected='oneArg<Map>({"a": 1, "b": 2})' /]

[@assertEquals actual=obj.oneArg2(m) expected='oneArg2<Map>({"a": 1, "b": 2})' /]
[@assertEquals actual=obj.oneArg2(ls) expected='oneArg2<List>([1, 2])' /]
[@assertFails message=noMatchingMsg]${obj.oneArg2(null)}[/@] (should be ambiguous)

[@assertFails message=noMatchingMsg]${obj.oneArg3(null)}[/@] (should be oneArg3<List>(null))

[#-- This test is unpredictable. The overloaded method implementation is buggy, and the result depends on the order in
     which the reflection API returns the methods, which for example changes between J2SE 5 and 6.   
[@assertFails message=ambigousMsg]${obj.oneArg4(123)}[/@] (should be oneArg<Integer>)
--] 

[@assertEquals actual=obj.notOverloaded(ls) expected='notOverloaded<List>([1, 2])' /]
[@assertEquals actual=obj.notOverloaded(null) expected='notOverloaded<List>(null)' /]

[@assertEquals actual=obj.varargsIssue1(m, []) expected='varargsIssue1<Map, List>({"a": 1, "b": 2}, [])' /] 
[@assertEquals actual=obj.varargsIssue1(m, null) expected='varargsIssue1<Object...>({"a": 1, "b": 2}, null)' /] (should be varargsIssue1<Map, List>)

[@assertEquals actual=obj.varargsIssue2("s", m) expected='varargsIssue2<String, Map>("s", {"a": 1, "b": 2})' /]
[@assertEquals actual=obj.varargsIssue2("s", ls) expected='varargsIssue2<String, List>("s", [1, 2])' /]
[@assertEquals actual=obj.varargsIssue2("s", null) expected='varargsIssue2<Object...>("s", null)' /] (should be an error?)

[@assertEquals actual=obj.numberIssue1(1) expected='numberIssue1<int>(1)' /]
[@assertEquals actual=obj.numberIssue1(0.5) expected='numberIssue1<int>(0)' /] (should be numberIssue1<float>(0.5))
[@assertEquals actual=obj.numberIssue1(0.5?float) expected='numberIssue1<float>(0.5)' /]
[@assertFails message=noMatchingMsg]${obj.numberIssue1(0.5?double)}[/@]
[@assertFails message=noMatchingMsg]${obj.numberIssue1(null)}[/@]

[@assertFails message=ambigousMsg]${obj.numberIssue2(1)}[/@]
[@assertFails message=ambigousMsg]${obj.numberIssue2(0.5)}[/@] (should be numberIssue2<BigDecimal>(0.5))
[@assertFails message=noMatchingMsg]${obj.numberIssue2(null)}[/@] (should be numberIssue2<BigDecimal>(null))

[@assertEquals actual=obj.numberIssue3(0.5) expected='numberIssue3<int>(0)' /] (should be numberIssue1<double>(0.5))
[@assertEquals actual=obj.numberIssue3(0.5?double) expected='numberIssue3<double>(0.5)' /]
[@assertEquals actual=obj.numberIssue3(0.5?float) expected='numberIssue3<double>(0.5)' /]

[@assertEquals actual="freemarker.test.templatesuite.models.OverloadedConstructor"?new(123) expected="int 123" /]
[@assertEquals actual="freemarker.test.templatesuite.models.OverloadedConstructor"?new("foo") expected="String foo" /]
[@assertFails message=noMatchingMsg]${"freemarker.test.templatesuite.models.OverloadedConstructor"?new(null)}[/@]
