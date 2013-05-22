package freemarker.ext.jsp;

import javax.servlet.jsp.PageContext;

import freemarker.core.Environment;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * @author Attila Szegedi
 * @version $Id: PageContextFactory.java,v 1.2 2005/06/11 21:21:09 szegedia Exp $
 */
class PageContextFactory {
    private static final Class pageContextImpl = getPageContextImpl();
    
    private static Class getPageContextImpl() {
        try {
            try {
                PageContext.class.getMethod("getELContext", (Class[]) null);
                return Class.forName("freemarker.ext.jsp.Internal_FreeMarkerPageContext21");
            }
            catch(NoSuchMethodException e1) {
                try {
                    PageContext.class.getMethod("getExpressionEvaluator", (Class[]) null);
                    return Class.forName("freemarker.ext.jsp.Internal_FreeMarkerPageContext2");
                }
                catch(NoSuchMethodException e2) {
                    return Class.forName("freemarker.ext.jsp.Internal_FreeMarkerPageContext1");
                }
            }
        }
        catch(ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    static FreeMarkerPageContext getCurrentPageContext() throws TemplateModelException {
        Environment env = Environment.getCurrentEnvironment();
        TemplateModel pageContextModel = env.getGlobalVariable(PageContext.PAGECONTEXT);
        if(pageContextModel instanceof FreeMarkerPageContext) {
            return (FreeMarkerPageContext)pageContextModel;
        }
        try {
            FreeMarkerPageContext pageContext = 
                (FreeMarkerPageContext)pageContextImpl.newInstance();
            env.setGlobalVariable(PageContext.PAGECONTEXT, pageContext);
            return pageContext;
        }
        catch(IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
        catch(InstantiationException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
}
