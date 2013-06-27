package freemarker.ext.jython;

import org.python.core.PyInstance;
import org.python.core.PyObject;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * {@link JythonVersionAdapter} for Jython 2.5.
 */
public class _Jython25VersionAdapter extends JythonVersionAdapter {

    public boolean isPyInstance(Object obj) {
        return obj instanceof PyInstance;
    }

    public Object pyInstanceToJava(Object pyInstance) {
        return ((PyInstance) pyInstance).__tojava__(java.lang.Object.class);
    }

    public String getPythonClassName(PyObject pyObject) {
        return pyObject.getType().getName();
    }

}
