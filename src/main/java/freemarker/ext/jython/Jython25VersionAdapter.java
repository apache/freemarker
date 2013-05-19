package freemarker.ext.jython;

import org.python.core.PyInstance;
import org.python.core.PyObject;

/**
 * {@link JythonVersionAdapter} for Jython 2.5.
 */
public class Jython25VersionAdapter extends JythonVersionAdapter {

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
