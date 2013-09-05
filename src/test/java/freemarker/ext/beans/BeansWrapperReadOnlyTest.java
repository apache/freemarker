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
import freemarker.template.utility.Lockable;

/**
 * Tests if all JavaBean properties of the standard {@link ObjectWrapper} classes are locked by
 * {@link Lockable#makeReadOnly()}. 
 */
public class BeansWrapperReadOnlyTest extends TestCase {

    private static final String EXPECTED_MESSAGE_PART = "read-only";

    public BeansWrapperReadOnlyTest(String name) {
        super(name);
    }
    
    public void testBeansWrapper() throws Exception {
        BeansWrapper bw = new BeansWrapper();
        bw.makeReadOnly();
        checkAllPropertiesReadOnly(bw);
    }

    public void testDefaultObjectWrapper() throws Exception {
        BeansWrapper bw = new DefaultObjectWrapper();
        bw.makeReadOnly();
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
