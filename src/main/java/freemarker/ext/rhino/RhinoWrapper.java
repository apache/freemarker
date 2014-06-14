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

package freemarker.ext.rhino;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.Wrapper;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 */
public class RhinoWrapper extends BeansWrapper {
    // The type of the "instance" field changed between Rhino versions, so a
    // GETSTATIC with wrong type declaration would cause a NoSuchFieldError;
    // we're avoiding it by acquiring it reflectively.
    private static final Object UNDEFINED_INSTANCE;
    static {
        try {
            UNDEFINED_INSTANCE = AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Undefined.class.getField("instance").get(null);
                }
            });
        }
        catch(RuntimeException e) {
            throw e;
        }
        catch(Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        // So our existence builtins work as expected.
        if(obj == UNDEFINED_INSTANCE || obj == UniqueTag.NOT_FOUND) {
            return null;
        }
        // UniqueTag.NULL_VALUE represents intentionally set null in Rhino, and
        // BeansWrapper#nullModel also represents intentionally returned null.
        // I [A.Sz.] am fairly certain that this value is never passed out of
        // any of the Rhino code back to clients, but is instead always being
        // converted back to null. However, since this object is available to 
        // any 3rd party Scriptable implementations as well, they might return
        // it, so we'll just be on the safe side, and handle it.
        if(obj == UniqueTag.NULL_VALUE) {
            return super.wrap(null);
        }
        // So, say, a JavaAdapter for FreeMarker interfaces works
        if(obj instanceof Wrapper) {
            obj = ((Wrapper)obj).unwrap();
        }
        return super.wrap(obj);
    }

    protected ModelFactory getModelFactory(Class clazz) {
        if(Scriptable.class.isAssignableFrom(clazz)) {
            return RhinoScriptableModel.FACTORY;
        }
        return super.getModelFactory(clazz);
    }
}
