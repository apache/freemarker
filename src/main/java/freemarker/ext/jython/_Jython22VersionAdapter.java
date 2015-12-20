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

import java.lang.reflect.Method;

import org.python.core.PyObject;
import org.python.core.PyType;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * {@link JythonVersionAdapter} for Jython 2.2.
 */
public class _Jython22VersionAdapter extends JythonVersionAdapter {

    @Override
    public boolean isPyInstance(Object obj) {
        return obj != null && getPyInstanceClass().isAssignableFrom(obj.getClass());
    }

    @Override
    public Object pyInstanceToJava(Object pyInstance) {
        try {
            // java.lang.Object org.python.core.PyInstance#__tojava__(java.lang.Class)
            final Method toJavaMethod = getPyInstanceClass().getMethod("__tojava__", Class.class);
            return toJavaMethod.invoke(pyInstance, Object.class);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Fail to invoke pyInstance.__tojava__() - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @Override
    public String getPythonClassName(PyObject pyObject) {
        try {
            // org.python.core.PyType org.python.core.PyObject#getType()
            Method getTypeMethod = PyObject.class.getMethod("getType");
            PyType pyType = (PyType) getTypeMethod.invoke(pyObject);
            // java.lang.String org.python.core.PyType#getFullName()
            Method getFullNameMethod = PyType.class.getMethod("getFullName");
            return (String) getFullNameMethod.invoke(pyType);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Fail to invoke pyObject.getType().getFullName() - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @Override
    protected String getPyInstanceClassName() {
        return "org.python.core.PyJavaInstance";
    }
}
