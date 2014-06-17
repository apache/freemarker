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

package freemarker.ext.beans;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.WriteProtectable;

/**
 * Tests if all JavaBean properties of the standard {@link ObjectWrapper} classes are locked by
 * {@link WriteProtectable#writeProtect()}. 
 */
public class BeansWrapperReadOnlyTest extends TestCase {

    private static final String EXPECTED_MESSAGE_PART = "write protected";

    public BeansWrapperReadOnlyTest(String name) {
        super(name);
    }
    
    public void testBeansWrapper() throws Exception {
        BeansWrapper bw = new BeansWrapper();
        bw.writeProtect();
        checkAllPropertiesReadOnly(bw);
    }

    public void testDefaultObjectWrapper() throws Exception {
        BeansWrapper bw = new DefaultObjectWrapper();
        bw.writeProtect();
        checkAllPropertiesReadOnly(bw);        
    }
    
    private void checkAllPropertiesReadOnly(Object o) throws Exception {
        BeanInfo bi = Introspector.getBeanInfo(o.getClass());
        for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
            Method setter = pd.getWriteMethod();
            if (setter != null) {
                Class t = pd.getPropertyType();
                
                Object val;
                if (ClassUtil.isNumerical(t)) {
                    val = Byte.valueOf((byte) 1);
                } else if (t == boolean.class) {
                    val = Boolean.TRUE;
                } else if (t == char.class) {
                    val = Character.valueOf('c');
                } else if (t.isAssignableFrom(String.class)) {
                    val = "s";
                } else {
                    val = null;
                }
                try {
                    setter.invoke(o, val);
                    fail("This setter should have failed as the property should be read-only now: " + setter);
                } catch (InvocationTargetException e) {
                    Throwable target = e.getTargetException();
                    if (!(target instanceof IllegalStateException
                            && target.getMessage() != null
                            && target.getMessage().contains(EXPECTED_MESSAGE_PART))) {
                        fail("Expected IllegalStateException with message containing \"" + EXPECTED_MESSAGE_PART
                                + "\", got this instead: " + target);
                    }
                }
            }
        }
    }

}
