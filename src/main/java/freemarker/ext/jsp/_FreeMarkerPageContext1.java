package freemarker.ext.jsp;

import freemarker.template.TemplateModelException;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * Implementation of PageContext that contains JSP 1.1 specific methods.
 * 
 * @author Attila Szegedi
 */
public class _FreeMarkerPageContext1 extends FreeMarkerPageContext {

    public _FreeMarkerPageContext1() throws TemplateModelException {
        super();
    }

    public void include (String s, boolean b) {}
}
