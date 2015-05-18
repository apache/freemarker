<!-- Test loading from web.xml-mapped JAR -->
<#assign tl2 = JspTaglibs["http://freemarker.sf.net/taglibs/freemarker-junit-test-tag-2.2-2"]>
<@tl2.testtag></@>
<!-- Test loading from autodeployed WEB-INF/lib/*.jar -->
<#assign tl3 = JspTaglibs["http://freemarker.sf.net/taglibs/freemarker-junit-test-tag-2.2-foo"]>
<@tl3.testtag></@>
<!-- Test loading from FreemarkerServlet "ClasspathTlds" -->
<@JspTaglibs["http://freemarker.org/taglibs/test/ClassPathTlds-1"].simpletag/>
<@JspTaglibs["http://freemarker.org/taglibs/test/ClassPathTlds-2"].simpletag/>
<#attempt><#assign _ = JspTaglibs["http://freemarker.org/taglibs/test/MetaInfTldSources-1"]><#recover>missing</#attempt>
<!-- Test loading from "MetaInfTldSources", inherited from Jetty -->
${JspTaglibs["http://java.sun.com/jsp/jstl/functions"].join(['a', 'b'], '+')}
<!-- Test loading from "MetaInfTldSources", set via init-param -->
<#attempt><#assign _ = JspTaglibs["http://displaytag.sf.net"]><#recover>missing</#attempt>
