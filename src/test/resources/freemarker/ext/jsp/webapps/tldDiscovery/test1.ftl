<#assign tl = JspTaglibs["http://freemarker.sf.net/taglibs/freemarker-junit-test-tag-2.2"]>

<!-- Test repeated execution -->
<@tl.testtag repeatCount=3 throwException=false
>Blah
</@>

<!-- Test 0-time execution -->
<@tl.testtag repeatCount=0 throwException=false
>Blah
</@>

<!-- Test abrupt execution -->
<@tl.testtag repeatCount=0 throwException=true
>Blah
</@>

<!-- Test nested execution -->
<@tl.testtag repeatCount=2 throwException=false
>Outer Blah
<@tl.testtag repeatCount=2 throwException=false
>Inner Blah
</@>
</@>

<!-- Test nested execution with intermittent non-JSP transform -->
<@tl.testtag repeatCount=2 throwException=false>
Outer Blah
<@compress>
<@tl.testtag repeatCount=2 throwException=false>
Inner Blah
</@>
</@>
</@>

<@tl.simpletag bodyLoopCount=2 name="simpletag1">
foo
<@tl.simpletag bodyLoopCount=3 name="simpletag2">
bar
</@>
</@>

<!-- Test loading from JAR -->
<#assign tl2 = JspTaglibs["http://freemarker.sf.net/taglibs/freemarker-junit-test-tag-2.2-2"]>
<@tl2.testtag></@>
<!-- Test loading from autodeployed JAR -->
<#assign tl3 = JspTaglibs["http://freemarker.sf.net/taglibs/freemarker-junit-test-tag-2.2-foo"]>
<@tl3.testtag></@>
<!-- Test loading from root-relative URL -->
<#assign tl4 = JspTaglibs["/WEB-INF/taglib2.jar"]>
<@tl4.testtag></@>
<!-- Test loading from non-root-relative URL -->
<#assign tl5 = JspTaglibs["WEB-INF/taglib2.jar"]>
<@tl5.testtag></@>
<!-- Test loading from autodeployed .tld -->
<#assign tl6 = JspTaglibs["http://freemarker.sf.net/taglibs/freemarker-junit-test-tag-autodeploy-tld-2"]>
<@tl6.simpletag/>
<#assign tl7 = JspTaglibs["http://freemarker.sf.net/taglibs/freemarker-junit-test-tag-autodeploy-tld-3"]>
<@tl7.simpletag/>
<!-- Test loading from FreemarkerServlet "ClasspathTlds" -->
<@JspTaglibs["http://freemarker.org/taglibs/test/ClassPathTlds-1"].simpletag/>
<@JspTaglibs["http://freemarker.org/taglibs/test/ClassPathTlds-2"].simpletag/>
<@JspTaglibs["http://freemarker.org/taglibs/test/ClassPathTlds-3"].simpletag/>
<!-- Test loading from "ClasspathTaglibJarPatterns", inherited from Jetty -->
${JspTaglibs["http://java.sun.com/jsp/jstl/functions"].join(['a', 'b'], '+')}
<!-- Test loading from "ClasspathTaglibJarPatterns", set via init-param -->
<#assign display = JspTaglibs["http://displaytag.sf.net"]>
<@display.table name="lsob">
  <@display.column property="name" />
  <@display.column property="age" />
  <@display.column property="maried" />
</@display.table>

<!-- Test loading from mapped relative URL -->
<@JspTaglibs["/subdir/taglib"].simpletag />

${tl.reverse("abc")}
${tl.reverseInt(123)}
${tl.reverseIntRadix(123, 2)}