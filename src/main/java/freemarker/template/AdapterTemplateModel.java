package freemarker.template;

/**
 * Supplemental interface that can be implemented by classes that also implement
 * any of the {@link TemplateModel} interfaces. A class implementing this 
 * interface usually serves as an adapter that provides bridging between a 
 * different object model and FreeMarker template models. It provides a 
 * capability to retrieve the underlying object. This interface is rarely 
 * implemented by applications. It is tipically implemented by adapter classes
 * used for wrapping in various object wrapper implementation.  
 * @author Attila Szegedi
 * @version $Id: AdapterTemplateModel.java,v 1.1 2005/06/12 19:03:07 szegedia Exp $
 */
public interface AdapterTemplateModel extends TemplateModel {
    /**
     * Retrieves the underlying object, or some other object semantically 
     * equivalent to its value narrowed by the class hint.   
     * @param hint the desired class of the returned value. An implementation 
     * should make reasonable effort to retrieve an object of the requested 
     * class, but if that is impossible, it must at least return the underlying 
     * object as-is. As a minimal requirement, an implementation must always 
     * return the exact underlying object when 
     * <tt>hint.isInstance(underlyingObject) == true</tt> holds. When called 
     * with <tt>java.lang.Object.class</tt>, it should return a generic Java 
     * object (i.e. if the model is wrapping a scripting lanugage object that is
     * further wrapping a Java object, the deepest underlying Java object should
     * be returned). 
     * @return the underlying object, or its value accommodated for the hint
     * class.
     */
    public Object getAdaptedObject(Class hint);
}
