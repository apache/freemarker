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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.python.core.PyClass;
import org.python.core.PyObject;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * {@link JythonVersionAdapter} for Jython 2.0 and 2.1.
 */
public class _Jython20And21VersionAdapter extends JythonVersionAdapter {

    @Override
    public boolean isPyInstance(Object obj) {
        return obj != null && getPyInstanceClass().isAssignableFrom(obj.getClass());
    }

    @Override
    public Object pyInstanceToJava(Object pyInstance) {
        try {
            // java.lang.Object org.python.core.PyInstance#__tojava__(java.lang.Class);
            final Method toJavaMethod = getPyInstanceClass().getMethod("__tojava__", Class.class);
            return toJavaMethod.invoke(pyInstance, Object.class);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Fail to invoke pyInstance.__tojava__(): - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @Override
    public String getPythonClassName(PyObject pyObject) {
        try {
            // org.python.core.PyClass org.python.core.PyObject#__class__
            Field pyClassField = PyObject.class.getField("__class__");
            // java.lang.String org.python.core.PyClass#__name__
            Field pyClassNameField = PyClass.class.getField("__name__");
            PyClass pyClass = (PyClass) pyClassField.get(pyObject);
            return (String) pyClassNameField.get(pyClass);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Fail to read pyObject.__class__.__name__: - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @Override
    protected String getPyInstanceClassName() {
        return "org.python.core.PyJavaInstance";
    }
}
