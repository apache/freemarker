<#ftl ns_prefixes={ "n": "http://freemarker.org/test/bar", "D": "http://freemarker.org/test/namespace-test" }>
<#global libResult>//n:e: ${doc['//n:e']}, ${doc.root['n:e']}</#global>
<#macro m>
//n:e: ${doc['//n:e']}, ${doc.root['n:e']}
</#macro>