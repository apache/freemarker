/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.TemplateException;
import freemarker.template.utility.NumberUtil;
import freemarker.template.utility.OptimizerUtil;
import freemarker.template.utility.StringUtil;

/**
 * Class to perform arithmetic operations.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 * @author Attila Szegedi
 */

public abstract class ArithmeticEngine {

    /**
     * Arithmetic engine that converts all numbers to {@link BigDecimal} and
     * then operates on them. This is FreeMarker's default arithmetic engine.
     */
    public static final BigDecimalEngine BIGDECIMAL_ENGINE = new BigDecimalEngine();
    /**
     * Arithmetic engine that uses (more-or-less) the widening conversions of
     * Java language to determine the type of result of operation, instead of
     * converting everything to BigDecimal up front.
     */
    public static final ConservativeEngine CONSERVATIVE_ENGINE = new ConservativeEngine();

    public abstract int compareNumbers(Number first, Number second) throws TemplateException;
    public abstract Number add(Number first, Number second) throws TemplateException;
    public abstract Number subtract(Number first, Number second) throws TemplateException;
    public abstract Number multiply(Number first, Number second) throws TemplateException;
    public abstract Number divide(Number first, Number second) throws TemplateException;
    public abstract Number modulus(Number first, Number second) throws TemplateException;
    public abstract Number toNumber(String s);

    protected int minScale = 12;
    protected int maxScale = 12;
    protected int roundingPolicy = BigDecimal.ROUND_HALF_UP;

    /**
     * Sets the minimal scale to use when dividing BigDecimal numbers. Default
     * value is 12.
     */
    public void setMinScale(int minScale) {
        if(minScale < 0) {
            throw new IllegalArgumentException("minScale < 0");
        }
        this.minScale = minScale;
    }
    
    /**
     * Sets the maximal scale to use when multiplying BigDecimal numbers. 
     * Default value is 100.
     */
    public void setMaxScale(int maxScale) {
        if(maxScale < minScale) {
            throw new IllegalArgumentException("maxScale < minScale");
        }
        this.maxScale = maxScale;
    }

    public void setRoundingPolicy(int roundingPolicy) {
        if (roundingPolicy != BigDecimal.ROUND_CEILING
            && roundingPolicy != BigDecimal.ROUND_DOWN
            && roundingPolicy != BigDecimal.ROUND_FLOOR
            && roundingPolicy != BigDecimal.ROUND_HALF_DOWN
            && roundingPolicy != BigDecimal.ROUND_HALF_EVEN
            && roundingPolicy != BigDecimal.ROUND_HALF_UP
            && roundingPolicy != BigDecimal.ROUND_UNNECESSARY
            && roundingPolicy != BigDecimal.ROUND_UP) 
        {
            throw new IllegalArgumentException("invalid rounding policy");        
        }
        
        this.roundingPolicy = roundingPolicy;
    }

    /**
     * This is the default arithmetic engine in FreeMarker. It converts every
     * number it receives into {@link BigDecimal}, then operates on these
     * converted {@link BigDecimal}s.
     */
    public static class BigDecimalEngine
    extends
        ArithmeticEngine    
    {
        public int compareNumbers(Number first, Number second) {
            // We try to find the result based on the sign (+/-/0) first, because:
            // - It's much faster than converting to BigDecial, and comparing to 0 is the most common comparison.
            // - It doesn't require any type conversions, and thus things like "Infinity > 0" won't fail.
            int firstSignum = NumberUtil.getSignum(first); 
            int secondSignum = NumberUtil.getSignum(second);
            if (firstSignum != secondSignum) {
                return firstSignum < secondSignum ? -1 : (firstSignum > secondSignum ? 1 : 0); 
            } else if (firstSignum == 0 && secondSignum == 0) {
                return 0;
            } else {
                BigDecimal left = toBigDecimal(first);
                BigDecimal right = toBigDecimal(second);
                return left.compareTo(right);
            }
        }
    
        public Number add(Number first, Number second) {
            BigDecimal left = toBigDecimal(first);
            BigDecimal right = toBigDecimal(second);
            return left.add(right);
        }
    
        public Number subtract(Number first, Number second) {
            BigDecimal left = toBigDecimal(first);
            BigDecimal right = toBigDecimal(second);
            return left.subtract(right);
        }
    
        public Number multiply(Number first, Number second) {
            BigDecimal left = toBigDecimal(first);
            BigDecimal right = toBigDecimal(second);
            BigDecimal result = left.multiply(right);
            if (result.scale() > maxScale) {
                result = result.setScale(maxScale, roundingPolicy);
            }
            return result;
        }
    
        public Number divide(Number first, Number second) {
            BigDecimal left = toBigDecimal(first);
            BigDecimal right = toBigDecimal(second);
            return divide(left, right);
        }
    
        public Number modulus(Number first, Number second) {
            long left = first.longValue();
            long right = second.longValue();
            return new Long(left % right);
        }
    
        public Number toNumber(String s) {
            return new BigDecimal(s);
        }
        
        private BigDecimal divide(BigDecimal left, BigDecimal right) {
            int scale1 = left.scale();
            int scale2 = right.scale();
            int scale = Math.max(scale1, scale2);
            scale = Math.max(minScale, scale);
            return left.divide(right, scale, roundingPolicy);
        }
    }

    /**
     * An arithmetic engine that conservatively widens the operation arguments
     * to extent that they can hold the result of the operation. Widening 
     * conversions occur in following situations:
     * <ul>
     * <li>byte and short are always widened to int (alike to Java language).</li>
     * <li>To preserve magnitude: when operands are of different types, the 
     * result type is the type of the wider operand.</li>
     * <li>to avoid overflows: if add, subtract, or multiply would overflow on
     * integer types, the result is widened from int to long, or from long to 
     * BigInteger.</li>
     * <li>to preserve fractional part: if a division of integer types would 
     * have a fractional part, int and long are converted to double, and 
     * BigInteger is converted to BigDecimal. An operation on a float and a 
     * long results in a double. An operation on a float or double and a
     * BigInteger results in a BigDecimal.</li>
     * </ul>
     */
    public static class ConservativeEngine extends ArithmeticEngine {
        private static final int INTEGER = 0;
        private static final int LONG = 1;
        private static final int FLOAT = 2;
        private static final int DOUBLE = 3;
        private static final int BIGINTEGER = 4;
        private static final int BIGDECIMAL = 5;
        
        private static final Map classCodes = createClassCodesMap();
        
        public int compareNumbers(Number first, Number second) throws TemplateException {
            switch(getCommonClassCode(first, second)) {
                case INTEGER: {
                    int n1 = first.intValue();
                    int n2 = second.intValue();
                    return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
                }
                case LONG: {
                    long n1 = first.longValue();
                    long n2 = second.longValue();
                    return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
                }
                case FLOAT: {
                    float n1 = first.floatValue();
                    float n2 = second.floatValue();
                    return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
                }
                case DOUBLE: {
                    double n1 = first.doubleValue();
                    double n2 = second.doubleValue();
                    return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
                }
                case BIGINTEGER: {
                    BigInteger n1 = toBigInteger(first);
                    BigInteger n2 = toBigInteger(second);
                    return n1.compareTo(n2);
                }
                case BIGDECIMAL: {
                    BigDecimal n1 = toBigDecimal(first);
                    BigDecimal n2 = toBigDecimal(second);
                    return n1.compareTo(n2);
                }
            }
            // Make the compiler happy. getCommonClassCode() is guaranteed to 
            // return only above codes, or throw an exception.
            throw new Error();
        }
    
        public Number add(Number first, Number second) throws TemplateException {
            switch(getCommonClassCode(first, second)) {
                case INTEGER: {
                    int n1 = first.intValue();
                    int n2 = second.intValue();
                    int n = n1 + n2;
                    return
                        ((n ^ n1) < 0 && (n ^ n2) < 0) // overflow check
                        ? (Number)new Long(((long)n1) + n2)
                        : (Number)new Integer(n);
                }
                case LONG: {
                    long n1 = first.longValue();
                    long n2 = second.longValue();
                    long n = n1 + n2;
                    return
                        ((n ^ n1) < 0 && (n ^ n2) < 0) // overflow check
                        ? (Number)toBigInteger(first).add(toBigInteger(second))
                        : (Number)new Long(n);
                }
                case FLOAT: {
                    return new Float(first.floatValue() + second.floatValue());
                }
                case DOUBLE: {
                    return new Double(first.doubleValue() + second.doubleValue());
                }
                case BIGINTEGER: {
                    BigInteger n1 = toBigInteger(first);
                    BigInteger n2 = toBigInteger(second);
                    return n1.add(n2);
                }
                case BIGDECIMAL: {
                    BigDecimal n1 = toBigDecimal(first);
                    BigDecimal n2 = toBigDecimal(second);
                    return n1.add(n2);
                }
            }
            // Make the compiler happy. getCommonClassCode() is guaranteed to 
            // return only above codes, or throw an exception.
            throw new Error();
        }
    
        public Number subtract(Number first, Number second) throws TemplateException {
            switch(getCommonClassCode(first, second)) {
                case INTEGER: {
                    int n1 = first.intValue();
                    int n2 = second.intValue();
                    int n = n1 - n2;
                    return
                        ((n ^ n1) < 0 && (n ^ ~n2) < 0) // overflow check
                        ? (Number)new Long(((long)n1) - n2)
                        : (Number)new Integer(n);
                }
                case LONG: {
                    long n1 = first.longValue();
                    long n2 = second.longValue();
                    long n = n1 - n2;
                    return
                        ((n ^ n1) < 0 && (n ^ ~n2) < 0) // overflow check
                        ? (Number)toBigInteger(first).subtract(toBigInteger(second))
                        : (Number)new Long(n);
                }
                case FLOAT: {
                    return new Float(first.floatValue() - second.floatValue());
                }
                case DOUBLE: {
                    return new Double(first.doubleValue() - second.doubleValue());
                }
                case BIGINTEGER: {
                    BigInteger n1 = toBigInteger(first);
                    BigInteger n2 = toBigInteger(second);
                    return n1.subtract(n2);
                }
                case BIGDECIMAL: {
                    BigDecimal n1 = toBigDecimal(first);
                    BigDecimal n2 = toBigDecimal(second);
                    return n1.subtract(n2);
                }
            }
            // Make the compiler happy. getCommonClassCode() is guaranteed to 
            // return only above codes, or throw an exception.
            throw new Error();
        }
    
        public Number multiply(Number first, Number second) throws TemplateException {
            switch(getCommonClassCode(first, second)) {
                case INTEGER: {
                    int n1 = first.intValue();
                    int n2 = second.intValue();
                    int n = n1 * n2;
                    return
                        n1== 0 || n/n1 == n2 // overflow check
                        ? (Number)new Integer(n)
                        : (Number)new Long(((long)n1) * n2);
                }
                case LONG: {
                    long n1 = first.longValue();
                    long n2 = second.longValue();
                    long n = n1 * n2;
                    return
                        n1==0L || n / n1 == n2 // overflow check
                        ? (Number)new Long(n)
                        : (Number)toBigInteger(first).multiply(toBigInteger(second));
                }
                case FLOAT: {
                    return new Float(first.floatValue() * second.floatValue());
                }
                case DOUBLE: {
                    return new Double(first.doubleValue() * second.doubleValue());
                }
                case BIGINTEGER: {
                    BigInteger n1 = toBigInteger(first);
                    BigInteger n2 = toBigInteger(second);
                    return n1.multiply(n2);
                }
                case BIGDECIMAL: {
                    BigDecimal n1 = toBigDecimal(first);
                    BigDecimal n2 = toBigDecimal(second);
                    BigDecimal r = n1.multiply(n2);
                    return r.scale() > maxScale ? r.setScale(maxScale, roundingPolicy) : r;
                }
            }
            // Make the compiler happy. getCommonClassCode() is guaranteed to 
            // return only above codes, or throw an exception.
            throw new Error();
        }
    
        public Number divide(Number first, Number second) throws TemplateException {
            switch(getCommonClassCode(first, second)) {
                case INTEGER: {
                    int n1 = first.intValue();
                    int n2 = second.intValue();
                    if (n1 % n2 == 0) {
                        return new Integer(n1/n2);
                    }
                    return new Double(((double)n1)/n2);
                }
                case LONG: {
                    long n1 = first.longValue();
                    long n2 = second.longValue();
                    if (n1 % n2 == 0) {
                        return new Long(n1/n2);
                    }
                    return new Double(((double)n1)/n2);
                }
                case FLOAT: {
                    return new Float(first.floatValue() / second.floatValue());
                }
                case DOUBLE: {
                    return new Double(first.doubleValue() / second.doubleValue());
                }
                case BIGINTEGER: {
                    BigInteger n1 = toBigInteger(first);
                    BigInteger n2 = toBigInteger(second);
                    BigInteger[] divmod = n1.divideAndRemainder(n2);
                    if(divmod[1].equals(BigInteger.ZERO)) {
                        return divmod[0];
                    }
                    else {
                        BigDecimal bd1 = new BigDecimal(n1);
                        BigDecimal bd2 = new BigDecimal(n2);
                        return bd1.divide(bd2, minScale, roundingPolicy);
                    }
                }
                case BIGDECIMAL: {
                    BigDecimal n1 = toBigDecimal(first);
                    BigDecimal n2 = toBigDecimal(second);
                    int scale1 = n1.scale();
                    int scale2 = n2.scale();
                    int scale = Math.max(scale1, scale2);
                    scale = Math.max(minScale, scale);
                    return n1.divide(n2, scale, roundingPolicy);
                }
            }
            // Make the compiler happy. getCommonClassCode() is guaranteed to 
            // return only above codes, or throw an exception.
            throw new Error();
        }
    
        public Number modulus(Number first, Number second) throws TemplateException {
            switch(getCommonClassCode(first, second)) {
                case INTEGER: {
                    return new Integer(first.intValue() % second.intValue());
                }
                case LONG: {
                    return new Long(first.longValue() % second.longValue());
                }
                case FLOAT: {
                    return new Float(first.floatValue() % second.floatValue());
                }
                case DOUBLE: {
                    return new Double(first.doubleValue() % second.doubleValue());
                }
                case BIGINTEGER: {
                    BigInteger n1 = toBigInteger(first);
                    BigInteger n2 = toBigInteger(second);
                    return n1.mod(n2);
                }
                case BIGDECIMAL: {
                    throw new _MiscTemplateException("Can't calculate remainder on BigDecimals");
                }
            }
            // Make the compiler happy. getCommonClassCode() is guaranteed to 
            // return only above codes, or throw an exception.
            throw new Error();
        }
    
        public Number toNumber(String s) {
            return OptimizerUtil.optimizeNumberRepresentation(new BigDecimal(s));
        }
        
        private static Map createClassCodesMap() {
            Map map = new HashMap(17);
            Integer intcode = new Integer(INTEGER);
            map.put(Byte.class, intcode);
            map.put(Short.class, intcode);
            map.put(Integer.class, intcode);
            map.put(Long.class, new Integer(LONG));
            map.put(Float.class, new Integer(FLOAT));
            map.put(Double.class, new Integer(DOUBLE));
            map.put(BigInteger.class, new Integer(BIGINTEGER));
            map.put(BigDecimal.class, new Integer(BIGDECIMAL));
            return map;
        }
        
        private static int getClassCode(Number num) throws TemplateException {
            try {
                return ((Integer)classCodes.get(num.getClass())).intValue();
            }
            catch(NullPointerException e) {
                if(num == null) {
                    throw new _MiscTemplateException("The Number object was null.");
                } else {
                    throw new _MiscTemplateException(new Object[] {
                            "Unknown number type ", num.getClass().getName() });
                }
            }
        }
        
        private static int getCommonClassCode(Number num1, Number num2) throws TemplateException {
            int c1 = getClassCode(num1);
            int c2 = getClassCode(num2);
            int c = c1 > c2 ? c1 : c2;
            // If BigInteger is combined with a Float or Double, the result is a
            // BigDecimal instead of BigInteger in order not to lose the 
            // fractional parts. If Float is combined with Long, the result is a
            // Double instead of Float to preserve the bigger bit width.
            switch(c) {
                case FLOAT: {
                    if((c1 < c2 ? c1 : c2) == LONG) {
                        return DOUBLE;
                    }
                    break;
                }
                case BIGINTEGER: {
                    int min = c1 < c2 ? c1 : c2;
                    if(min == DOUBLE || min == FLOAT) {
                        return BIGDECIMAL;
                    }
                    break;
                }
            }
            return c;
        }
        
        private static BigInteger toBigInteger(Number num) {
            return num instanceof BigInteger ? (BigInteger) num : new BigInteger(num.toString());
        }
    }

    private static BigDecimal toBigDecimal(Number num) {
        try {
            return num instanceof BigDecimal ? (BigDecimal) num : new BigDecimal(num.toString());
        } catch (NumberFormatException e) {
            // The exception message is useless, so we add a new one:
            throw new NumberFormatException("Can't parse this as BigDecimal number: " + StringUtil.jQuote(num));
        }
    }
}
