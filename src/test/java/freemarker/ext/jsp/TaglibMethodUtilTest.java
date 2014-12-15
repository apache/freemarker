package freemarker.ext.jsp;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

public class TaglibMethodUtilTest {

    @Test
    public void testGetMethodByFunctionSignature() throws Exception {
        Method expected = Functions.class.getMethod("plus", new Class [] { int.class, int.class });
        String signature = "int plus(int, int)";
        Method method = TaglibMethodUtil.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", new Class [] { double.class, double.class });
        signature = "double  plus ( double , double )";
        method = TaglibMethodUtil.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", new Class [] { String.class, String.class });
        signature = "java.lang.String plus ( java.lang.String  ,java.lang.String  )";
        method = TaglibMethodUtil.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", new Class [] { double[].class, double[].class });
        signature = "double[] plus ( double[]  ,double []  )";
        method = TaglibMethodUtil.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("plus", new Class [] { String[].class, String[].class });
        signature = "java.lang.String [] plus ( java.lang.String[]  ,java.lang.String []  )";
        method = TaglibMethodUtil.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);

        expected = Functions.class.getMethod("sum", new Class [] { double[].class });
        signature = "double sum ( double[]  )";
        method = TaglibMethodUtil.getMethodByFunctionSignature(Functions.class, signature);
        assertEquals(expected, method);
    }

    @SuppressWarnings("unused")
    private static class Functions {

        public static int plus(int a, int b) {
            return a + b;
        }

        public static double plus(double a, double b) {
            return a + b;
        }

        public static String plus(String a, String b) {
            return a + b;
        }

        public static double [] plus(double [] a, double [] b) { 
            double [] sum = new double[a.length];

            for (int i = 0; i < a.length; i++) {
                sum[i] = a[i] + b[i];
            }

            return sum;
        }

        public static String [] plus(String [] a, String [] b) { 
            String [] joins = new String[a.length];

            for (int i = 0; i < a.length; i++) {
                joins[i] = a[i] + b[i];
            }

            return joins;
        }

        public static double sum(double [] a) { 
            double sum = 0.0;
            for (int i = 0; i < a.length; i++) {
                sum += a[i];
            }
            return sum;
        }
    }
    
}
