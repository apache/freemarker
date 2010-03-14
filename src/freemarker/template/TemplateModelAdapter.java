package freemarker.template;

/**
 * Implemented by classes that serve as adapters for template model objects in
 * some other object model. Actually a functional inverse of 
 * {@link AdapterTemplateModel}. You will rarely implement this interface 
 * directly. It is usually implemented by unwrapping adapter classes of various 
 * object wrapper implementations.
 * @author Attila Szegedi
 * @version $Id: TemplateModelAdapter.java,v 1.1 2005/06/12 19:03:07 szegedia Exp $
 */
public interface TemplateModelAdapter {
    /**
     * @return the template model this object is wrapping.
     */
    public TemplateModel getTemplateModel();
}
