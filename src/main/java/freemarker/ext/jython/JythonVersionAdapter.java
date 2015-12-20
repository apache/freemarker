/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.ext.jython;

import org.python.core.PyObject;

/**
 * Functions that has a different implementation depending on the Jython version
 * used. This was introduced to work around class-loading errors because of
 * different classes/methods being present in different Jython versions.
 */
public abstract class JythonVersionAdapter {

    private volatile Class<?> pyInstanceClass;

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
     * Returns <code>pyObject.__class__.__name__</code> (Jython 2.0 and 2.1),
     * <code>pyObject.getType().getFullName()</code> (Jython 2.2), or
     * <code>pyObject.getType().getName()</code> (Jython 2.5). 
     */
    public abstract String getPythonClassName(PyObject pyObject);

    /**
     * Returns <code>Py[Java]Instance</code> class. This method can be used when an implementation needs to use Java
     * Reflection APIs on the <code>Py[Java]Instance</code> for a specific Jython version at runtime.
     * 
     * @return <code>Py[Java]Instance</code> class
     */
    protected Class<?> getPyInstanceClass() {
        Class<?> pyInstCls = pyInstanceClass;

        if (pyInstCls == null) {
            synchronized (this) {
                pyInstCls = pyInstanceClass;
                if (pyInstCls == null) {
                    try {
                        pyInstanceClass = pyInstCls = Class.forName(getPyInstanceClassName());
                    } catch (ClassNotFoundException ex) {
                        throw new IllegalStateException(
                                "PyInstance class not found: - " + ex.getClass().getName() + ": " + ex.getMessage());
                    }
                }
            }
        }

        return pyInstCls;
    }

    /**
     * Returns the fully qualified name of <code>Py[Java]Instance</code> class. This method is invoked by
     * {@link #getPyInstanceClass()} when an implementation needs to use Java Reflection APIs on the
     * <code>Py[Java]Instance</code> for a specific Jython version at runtime.
     * 
     * @return the fully qualified name of <code>Py[Java]Instance</code> class
     */
    protected abstract String getPyInstanceClassName();

}
