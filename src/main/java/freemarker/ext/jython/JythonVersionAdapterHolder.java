package freemarker.ext.jython;

import org.python.core.PySystemState;

import freemarker.template.utility.StringUtil;

/**
 * Holds the {@link JythonVersionAdapter} so that it's only initialized when this class is accessed.
 * This utilizes that the JVM is required to initialize the fields of a class not earlier than the class is
 * first accessed. Furthermore, it utilizes that the JVM guarantees that the objects created as part of the class
 * initialization will be visible with their after-initialization state for the threads that access it. 
 */
class JythonVersionAdapterHolder {
    
    final static JythonVersionAdapter INSTANCE;
    static {
        // Note: Only the textual version number is available in Jython 2.0. 
        int version;
        try {
            // Although PySystemState.version is present in all versions,
            // its type changes, so we must use reflection to get it.
            version = StringUtil.versionStringToInt(
                    PySystemState.class.getField("version").get(null).toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Jython version: " + e);
        }
        ClassLoader cl = JythonVersionAdapter.class.getClassLoader();
        try {
            if (version >= 2005000) {
                INSTANCE = (JythonVersionAdapter) cl.loadClass(
                        "freemarker.ext.jython._Jython25VersionAdapter")
                    .newInstance();
            } else if (version >= 2002000) {
                INSTANCE = (JythonVersionAdapter) cl.loadClass(
                        "freemarker.ext.jython._Jython22VersionAdapter")
                    .newInstance();
            } else {
                INSTANCE = (JythonVersionAdapter) cl.loadClass(
                        "freemarker.ext.jython._Jython20And21VersionAdapter")
                    .newInstance();
            }
        } catch (ClassNotFoundException e) {
            throw adapterCreationException(e);
        } catch (IllegalAccessException e) {
            throw adapterCreationException(e);
        } catch (InstantiationException e) {
            throw adapterCreationException(e);
        }
    }

    private static RuntimeException adapterCreationException(Exception e) {
        return new RuntimeException(
                "Unexpected exception when creating JythonVersionAdapter: "
                + e); // No cause exception argument available on Java 1.2.; fix this with Java 5
    }

}
