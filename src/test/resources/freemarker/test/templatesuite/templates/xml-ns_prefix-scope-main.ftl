<#ftl ns_prefixes={
    "D": "http://freemarker.org/test/namespace-test",
    "n": "http://freemarker.org/test/foo",
    "bar": "http://freemarker.org/test/bar"
}>
//e: ${doc['//D:e']}, ${doc.root.e}
//n:e: ${doc['//n:e']}, ${doc.root['n:e']}
//bar:e: ${doc['//bar:e']}, ${doc.root['bar:e']}

Included:
<#include "xml-ns_prefix-scope-lib.ftl">
${libResult}
<@m />

Imported:
<#import "xml-ns_prefix-scope-lib.ftl" as lib>
${libResult}
<@lib.m />
