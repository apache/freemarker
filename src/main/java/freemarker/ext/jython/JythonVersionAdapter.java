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
