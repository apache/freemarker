package freemarker.template.utility;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Number- and math-related utilities.
 * 
 * @since 2.3.20
 */
public class NumberUtil {

    private NumberUtil() { }
    
    public static boolean isInfinite(Number num) {
        if (num instanceof Double) {
            return ((Double) num).isInfinite();
        } else if (num instanceof Float) {
            return ((Float) num).isInfinite();
        } else if (isNonFPNumberOfSupportedClass(num)) {
            return false;
        } else {
            throw new UnsupportedNumberClassException(num.getClass());
        }           
    }

    public static boolean isNaN(Number num) {
        if (num instanceof Double) {
            return ((Double) num).isNaN();
        } else if (num instanceof Float) {
            return ((Float) num).isNaN();
        } else if (isNonFPNumberOfSupportedClass(num)) {
            return false;
        } else {
            throw new UnsupportedNumberClassException(num.getClass());
        }           
    }

    /**
     * @return -1 for negative, 0 for zero, 1 for positive.
     * @throws ArithmeticException if the number is NaN
     */
    public static int getSignum(Number num) throws ArithmeticException {
        if (num instanceof Integer) {
            int n = ((Integer) num).intValue();
            return n > 0 ? 1 : (n == 0 ? 0 : -1);
        } else if (num instanceof BigDecimal) {
            BigDecimal n = (BigDecimal) num;
            return n.signum();
        } else if (num instanceof Double) {
            double n = ((Double) num).doubleValue();
            if (n > 0) return 1;
            else if (n == 0) return 0;
            else if (n < 0) return -1;
            else throw new ArithmeticException("The signum of " + n + " is not defined.");  // NaN
        } else if (num instanceof Float) {
            float n = ((Float) num).floatValue();
            if (n > 0) return 1;
            else if (n == 0) return 0;
            else if (n < 0) return -1;
            else throw new ArithmeticException("The signum of " + n + " is not defined.");  // NaN
        } else if (num instanceof Long) {
            long n = ((Long) num).longValue();
            return n > 0 ? 1 : (n == 0 ? 0 : -1);
        } else if (num instanceof Short) {
            short n = ((Short) num).shortValue();
            return n > 0 ? 1 : (n == 0 ? 0 : -1);
        } else if (num instanceof Byte) {
            byte n = ((Byte) num).byteValue();
            return n > 0 ? 1 : (n == 0 ? 0 : -1);
        } else if (num instanceof BigInteger) {
            BigInteger n = (BigInteger) num;
            return n.signum();
        } else {
            throw new UnsupportedNumberClassException(num.getClass());
        }
    }
    
    private static boolean isNonFPNumberOfSupportedClass(Number num) {
        return num instanceof Integer || num instanceof BigDecimal || num instanceof Long
                || num instanceof Short || num instanceof Byte || num instanceof BigInteger;
    }
    
}
