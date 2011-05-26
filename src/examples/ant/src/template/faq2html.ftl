<#escape x as x?html>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
  <head>
    <title>${.node.faq.@title}</title>
    <meta name="keywords" content="FreeMarker, Java, servlet, HTML, template, free software, open source, XML" />
  </head>
  <body bgcolor="#ffffff">
    <h2><a name="top">${.node.faq.@title}</a></h2>
    <#noescape>${.node.faq.preface.@@markup}</#noescape>
    <h3>Table of contents</h3>

    <#list .node.faq.topicgroup as topicgroup>
      <h4>${topicgroup.@name}</h4>
      <ul>
      <#list topicgroup.topic as topic>
        <li><a href="#${topic.@id}">${topic.@name}</a></li>
      </#list>
      </ul>
    </#list>

    <p>If your question was not answered by this FAQ, write to
    <a href="mailto:${.node.faq.@adminMailTo}">${.node.faq.@adminName}</a>
    (the current maintainer).</p>
    <#list .node.faq.topicgroup.topic as topic>
      <hr />
      <h3><a name="${topic.@id}">${topic.@name}</a></h3>
        <#noescape>${topic.@@nested_markup}</#noescape>
      <p><a href="#top">Back to top</a></p>
    </#list>
  </body>
</html>
</#escape>