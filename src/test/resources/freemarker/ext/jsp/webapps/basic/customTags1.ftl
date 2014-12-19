<#assign t = JspTaglibs["http://freemarker.org/test/taglibs/test"]>

<!-- Test repeated execution -->
<@t.testtag repeatCount=3 throwException=false
>Blah
</@>

<!-- Test 0-time execution -->
<@t.testtag repeatCount=0 throwException=false
>Blah
</@>

<!-- Test abrupt execution -->
<@t.testtag repeatCount=0 throwException=true
>Blah
</@>

<!-- Test nested execution -->
<@t.testtag repeatCount=2 throwException=false
>Outer Blah
<@t.testtag repeatCount=2 throwException=false
>Inner Blah
</@>
</@>

<!-- Test nested execution with intermittent non-JSP transform -->
<@t.testtag repeatCount=2 throwException=false>
Outer Blah
<@compress>
<@t.testtag repeatCount=2 throwException=false>
Inner Blah
</@>
</@>
</@>

<@t.simpletag bodyLoopCount=2 name="simpletag1">
foo
<@t.simpletag bodyLoopCount=3 name="simpletag2">
bar
</@>
</@>
