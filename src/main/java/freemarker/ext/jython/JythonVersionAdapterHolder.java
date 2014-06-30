/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        return new RuntimeException("Unexpected exception when creating JythonVersionAdapter", e);
    }

}
