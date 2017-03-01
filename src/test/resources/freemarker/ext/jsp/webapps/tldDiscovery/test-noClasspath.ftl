<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
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
