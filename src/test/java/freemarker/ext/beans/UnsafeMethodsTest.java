package freemarker.ext.beans;

import org.junit.Test;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import static org.junit.Assert.*;


public class UnsafeMethodsTest {

    @Test
    public void testUnsafeMethods() throws NoSuchMethodException {
        Method method = ProtectionDomain.class.getMethod("getClassLoader");
        assertTrue(UnsafeMethods.isUnsafeMethod(method));
    }

}