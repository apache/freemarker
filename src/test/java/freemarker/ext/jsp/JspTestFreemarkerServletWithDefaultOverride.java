package freemarker.ext.jsp;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

import freemarker.ext.jsp.TaglibFactory.ClasspathMetaInfTldSource;
import freemarker.ext.jsp.TaglibFactory.MetaInfTldSource;
import freemarker.ext.jsp.TaglibFactory.WebInfPerLibJarMetaInfTldSource;

public class JspTestFreemarkerServletWithDefaultOverride extends JspTestFreemarkerServlet {

    @Override
    protected List<String> createDefaultClassPathTlds() {
        return Collections.singletonList("/freemarker/ext/jsp/tldDiscovery-ClassPathTlds-1.tld");
    }

    @Override
    protected List<MetaInfTldSource> createDefaultMetaInfTldSources() {
        return ImmutableList.of(
                WebInfPerLibJarMetaInfTldSource.INSTANCE,
                new ClasspathMetaInfTldSource(Pattern.compile(".*displaytag.*\\.jar$")),
                new ClasspathMetaInfTldSource(Pattern.compile(".*")));
    }

}
