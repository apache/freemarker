package freemarker.ext.jython;

import org.python.core.PyObject;

/**
 * Functions that has a different implementation depending on the Jython version
 * used. This was introduced to work around class-loading errors because of
 * different classes/methods being present in different Jython versions.
 */
public abstract class JythonVersionAdapter {

    /**
     * Returns
     * <code>obj instanceof Py[Java]Instance</code>. 
     */
    public abstract boolean isPyInstance(Object obj);
    
    /**
     * Returns
     * <code>((PyInstance) py[Java]Instance).__tojava__(java.lang.Object.class)</code>.
     */
    public abstract Object pyInstanceToJava(Object pyInstance);
    
    /**
     * Returns <code>pyObject.__class__.__name__</code> 
     */
    public abstract String getPythonClassName(PyObject pyObject);
    
}
