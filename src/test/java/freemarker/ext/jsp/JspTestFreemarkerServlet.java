package freemarker.ext.jsp;

import javax.servlet.ServletContext;

import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModelException;

public class JspTestFreemarkerServlet extends FreemarkerServlet {

    static volatile boolean emulateNoUrlToFileConversions;
    static volatile boolean emulateNoJarURLConnections;
    static volatile boolean emulateJarEntryUrlOpenStreamFails;
    
    static void resetToDefaults() {
        emulateNoUrlToFileConversions = false;
        emulateNoJarURLConnections = false;
        emulateJarEntryUrlOpenStreamFails = false;
    }

    @Override
    protected TaglibFactory createTaglibFactory(ObjectWrapper objectWrapper, ServletContext servletContext)
            throws TemplateModelException {
        final TaglibFactory taglibFactory = super.createTaglibFactory(objectWrapper, servletContext);
        taglibFactory.test_emulateNoUrlToFileConversions = emulateNoUrlToFileConversions;
        taglibFactory.test_emulateNoJarURLConnections = emulateNoJarURLConnections;
        taglibFactory.test_emulateJarEntryUrlOpenStreamFails = emulateJarEntryUrlOpenStreamFails;
        return taglibFactory;
    }

}
