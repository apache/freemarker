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

package freemarker.template.utility;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Number- and math-related utilities.
 * 
 * @since 2.3.20
 */
public class NumberUtil {

    private static final BigDecimal BIG_DECIMAL_INT_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal BIG_DECIMAL_INT_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigInteger BIG_INTEGER_INT_MIN = BIG_DECIMAL_INT_MIN.toBigInteger();
    private static final BigInteger BIG_INTEGER_INT_MAX = BIG_DECIMAL_INT_MAX.toBigInteger();

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
    
    /**
     * Tells if a {@link BigDecimal} stores a whole number. For example, it returns {@code true} for {@code 1.0000},
     * but {@code false} for {@code 1.0001}.
     * 
     * @since 2.3.21
     */
    static public boolean isIntegerBigDecimal(BigDecimal bd) {
        // [Java 1.5] Try to utilize BigDecimal.toXxxExact methods
        return bd.scale() <= 0  // A fast check that whole numbers usually (not always) match
               || bd.setScale(0, BigDecimal.ROUND_DOWN).compareTo(bd) == 0;  // This is rather slow
        // Note that `bd.signum() == 0 || bd.stripTrailingZeros().scale() <= 0` was also tried for the last
        // condition, but stripTrailingZeros was slower than setScale + compareTo.
    }
    
    private static boolean isNonFPNumberOfSupportedClass(Number num) {
        return num instanceof Integer || num instanceof BigDecimal || num instanceof Long
                || num instanceof Short || num instanceof Byte || num instanceof BigInteger;
    }

    /**
     * Converts a {@link Number} to {@code int} whose mathematical value is exactly the same as of the original number.
     * 
     * @throws ArithmeticException
     *             if the conversion to {@code int} is not possible without losing precision or overflow/underflow.
     * 
     * @since 2.3.22
     */
    public static int toIntExact(Number num) {
        if (num instanceof Integer || num instanceof Short || num instanceof Byte) {
            return num.intValue();
        } else if (num instanceof Long) {
            final long n = num.longValue();
            final int result = (int) n;
            if (n != result) {
                throw newLossyConverionException(num, Integer.class);
            }
            return result;
        } else if (num instanceof Double || num instanceof Float) {
            final double n = num.doubleValue();
            if (n % 1 != 0 || n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) {
                throw newLossyConverionException(num, Integer.class);
            }
            return (int) n;
        } else if (num instanceof BigDecimal) {
            // [Java 1.5] Use BigDecimal.toIntegerExact()
            BigDecimal n = (BigDecimal) num;
            if (!isIntegerBigDecimal(n)
                    || n.compareTo(BIG_DECIMAL_INT_MAX) > 0 || n.compareTo(BIG_DECIMAL_INT_MIN) < 0) {
                throw newLossyConverionException(num, Integer.class);
            }
            return n.intValue();
        } else if (num instanceof BigInteger) {
            BigInteger n = (BigInteger) num;
            if (n.compareTo(BIG_INTEGER_INT_MAX) > 0 || n.compareTo(BIG_INTEGER_INT_MIN) < 0) {
                throw newLossyConverionException(num, Integer.class);
            }
            return n.intValue();
        } else {
            throw new UnsupportedNumberClassException(num.getClass());
        }
    }

    private static ArithmeticException newLossyConverionException(Number fromValue, Class/*<Number>*/ toType) {
        return new ArithmeticException(
                "Can't convert " + fromValue + " to type " + ClassUtil.getShortClassName(toType) + " without loss.");
    }
    
}
