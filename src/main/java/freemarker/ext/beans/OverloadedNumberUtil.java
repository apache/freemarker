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

import java.math.BigDecimal;
import java.math.BigInteger;

import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.NumberUtil;

/**
 * Everything related to coercion to ambiguous numerical types.  
 */
class OverloadedNumberUtil {

    // Can't be instantiated
    private OverloadedNumberUtil() { }

    /**
     * The lower limit of conversion prices where there's a risk of significant mantissa loss.
     * The value comes from misc/overloadedNumberRules/prices.ods and generator.ftl.
     */
    static final int BIG_MANTISSA_LOSS_PRICE = 4 * 10000;
    
    /** The highest long that can be stored in double without precision loss: 2**53. */
    private static final long MAX_DOUBLE_OR_LONG = 9007199254740992L;
    /** The lowest long that can be stored in double without precision loss: -(2**53). */
    private static final long MIN_DOUBLE_OR_LONG = -9007199254740992L;
    private static final int MAX_DOUBLE_OR_LONG_LOG_2 = 53;
    
    /** The highest long that can be stored in float without precision loss: 2**24. */
    private static final int MAX_FLOAT_OR_INT = 16777216;
    /** The lowest long that can be stored in float without precision loss: -(2**24). */
    private static final int MIN_FLOAT_OR_INT = -16777216;
    private static final int MAX_FLOAT_OR_INT_LOG_2 = 24;
    /** Lowest number that we don't thread as possible integer 0. */
    private static final double LOWEST_ABOVE_ZERO = 0.000001;
    /** Highest number that we don't thread as possible integer 1. */
    private static final double HIGHEST_BELOW_ONE = 0.999999;

    /**
     * Attaches the lowest alternative number type to the parameter number via {@link NumberWithFallbackType}, if
     * that's useful according the possible target number types. This transformation is applied on the method call
     * argument list before overloaded method selection.
     * 
     * <p>Note that as of this writing, this method is only used when
     * {@link BeansWrapper#getIncompatibleImprovements()} >= 2.3.21.
     * 
     * <p>Why's this needed, how it works: Overloaded method selection only selects methods where the <em>type</em>
     * (not the value!) of the argument is "smaller" or the same as the parameter type. This is similar to how it's in
     * the Java language. That it only decides based on the parameter type is important because this way
     * {@link OverloadedMethodsSubset} can cache method lookup decisions using the types as the cache key. Problem is,
     * since you don't declare the exact numerical types in FTL, and FTL has only a single generic numeric type
     * anyway, what Java type a {@link TemplateNumberModel} uses internally is often seen as a technical detail of which
     * the template author can't always keep track of. So we investigate the <em>value</em> of the number too,
     * then coerce it down without overflow to a type that will match the most overloaded methods. (This
     * is especially important as FTL often stores numbers in {@link BigDecimal}-s, which will hardly ever match any
     * method parameters.) We could simply return that number, like {@code Byte(0)} for an {@code Integer(0)},
     * however, then we would lose the information about what the original type was. The original type is sometimes
     * important, as in ambiguous situations the method where there's an exact type match should be selected (like,
     * when someone wants to select an overload explicitly with {@code m(x?int)}). Also, if an overload wins where
     * the parameter type at the position of the number is {@code Number} or {@code Object} (or {@code Comparable}
     * etc.), it's expected that we pass in the original value (an {@code Integer} in this example), especially if that
     * value is the return value of another Java method. That's why we use
     * {@link NumberWithFallbackType} numerical classes like {@link IntegerOrByte}, which represents both the original
     * type and the coerced type, all encoded into the class of the value, which is used as the overloaded method lookup
     * cache key.
     *  
     * <p>See also: <tt>src\main\misc\overloadedNumberRules\prices.ods</tt>.
     * 
     * @param num the number to coerce
     * @param typeFlags the type flags of the target parameter position; see {@link TypeFlags}
     * 
     * @returns The original number or a {@link NumberWithFallbackType}, depending on the actual value and the types
     *     indicated in the {@code targetNumTypes} parameter.
     */
    static Number addFallbackType(final Number num, final int typeFlags) {
        // Java 5: use valueOf where possible
        final Class numClass = num.getClass();
        if (numClass == BigDecimal.class) {
            // For now we only support the backward-compatible mode that doesn't prevent roll overs and magnitude loss.
            // However, we push the overloaded selection to the right direction, so we will at least indicate if the
            // number has decimals.
            BigDecimal n = (BigDecimal) num; 
            if ((typeFlags & TypeFlags.MASK_KNOWN_INTEGERS) != 0
                    && (typeFlags & TypeFlags.MASK_KNOWN_NONINTEGERS) != 0
                    && NumberUtil.isIntegerBigDecimal(n) /* <- can be expensive */) {
                return new IntegerBigDecimal(n);
            } else {
                // Either it was a non-integer, or it didn't mater what it was, as we don't have both integer and
                // non-integer target types. 
                return n;
            }
        } else if (numClass == Integer.class) {
            int pn = num.intValue();
            // Note that we try to return the most specific type (i.e., the numerical type with the smallest range), but
            // only among the types that are possible targets. Like if the only target is int and the value is 1, we
            // will return Integer 1, not Byte 1, even though byte is automatically converted to int so it would
            // work too. Why we avoid unnecessarily specific types is that they generate more overloaded method lookup
            // cache entries, since the cache key is the array of the types of the argument values. So we want as few
            // permutations as possible. 
            if ((typeFlags & TypeFlags.BYTE) != 0 && pn <= Byte.MAX_VALUE && pn >= Byte.MIN_VALUE) {
                return new IntegerOrByte((Integer) num, (byte) pn);
            } else if ((typeFlags & TypeFlags.SHORT) != 0 && pn <= Short.MAX_VALUE && pn >= Short.MIN_VALUE) {
                return new IntegerOrShort((Integer) num, (short) pn);
            } else {
                return num;
            }
        } else if (numClass == Long.class) {
            final long pn = num.longValue(); 
            if ((typeFlags & TypeFlags.BYTE) != 0 && pn <= Byte.MAX_VALUE && pn >= Byte.MIN_VALUE) {
                return new LongOrByte((Long) num, (byte) pn);
            } else if ((typeFlags & TypeFlags.SHORT) != 0 && pn <= Short.MAX_VALUE && pn >= Short.MIN_VALUE) {
                return new LongOrShort((Long) num, (short) pn);
            } else if ((typeFlags & TypeFlags.INTEGER) != 0 && pn <= Integer.MAX_VALUE && pn >= Integer.MIN_VALUE) {
                return new LongOrInteger((Long) num, (int) pn);
            } else {
                return num;
            }
        } else if (numClass == Double.class) {
            final double doubleN = num.doubleValue();
            
            // Can we store it in an integer type?
            checkIfWholeNumber: do {
                if ((typeFlags & TypeFlags.MASK_KNOWN_INTEGERS) == 0) break checkIfWholeNumber;
                
                // There's no hope to be 1-precise outside this region. (Although problems can occur even inside it...)
                if (doubleN > MAX_DOUBLE_OR_LONG || doubleN < MIN_DOUBLE_OR_LONG) break checkIfWholeNumber;
                
                long longN = num.longValue(); 
                double diff = doubleN - longN;
                boolean exact;  // We will try to ignore precision glitches (like 0.3 - 0.2 - 0.1 = -2.7E-17)
                if (diff == 0) {
                    exact = true;
                } else if (diff > 0) {
                    if (diff < LOWEST_ABOVE_ZERO) {
                        exact = false;
                    } else if (diff > HIGHEST_BELOW_ONE) {
                        exact = false;
                        longN++;
                    } else {
                        break checkIfWholeNumber;
                    }
                } else {  // => diff < 0
                    if (diff > -LOWEST_ABOVE_ZERO) {
                        exact = false;
                    } else if (diff < -HIGHEST_BELOW_ONE) {
                        exact = false;
                        longN--;
                    } else {
                        break checkIfWholeNumber;
                    }
                }
                
                // If we reach this, it can be treated as a whole number.
                
                if ((typeFlags & TypeFlags.BYTE) != 0
                        && longN <= Byte.MAX_VALUE && longN >= Byte.MIN_VALUE) {
                    return new DoubleOrByte((Double) num, (byte) longN);
                } else if ((typeFlags & TypeFlags.SHORT) != 0
                        && longN <= Short.MAX_VALUE && longN >= Short.MIN_VALUE) {
                    return new DoubleOrShort((Double) num, (short) longN);
                } else if ((typeFlags & TypeFlags.INTEGER) != 0
                        && longN <= Integer.MAX_VALUE && longN >= Integer.MIN_VALUE) {
                    final int intN = (int) longN; 
                    // Java 5: remove the "? (Number)" and ": (Number)" casts
                    return (typeFlags & TypeFlags.FLOAT) != 0 && intN >= MIN_FLOAT_OR_INT && intN <= MAX_FLOAT_OR_INT
                                    ? (Number) new DoubleOrIntegerOrFloat((Double) num, intN)
                                    : (Number) new DoubleOrInteger((Double) num, intN);
                } else if ((typeFlags & TypeFlags.LONG) != 0) {
                    if (exact) {
                        return new DoubleOrLong((Double) num, longN);
                    } else {
                        // We don't deal with non-exact numbers outside the range of int, as we already reach
                        // ULP 2.384185791015625E-7 there.
                        if (longN >= Integer.MIN_VALUE && longN <= Integer.MAX_VALUE) {
                            return new DoubleOrLong((Double) num, longN);
                        } else {
                            break checkIfWholeNumber;
                        }
                    }
                }
                // This point is reached if the double value was out of the range of target integer type(s). 
                // Falls through!
            } while (false);
            // If we reach this that means that it can't be treated as a whole number.
            
            if ((typeFlags & TypeFlags.FLOAT) != 0 && doubleN >= -Float.MAX_VALUE && doubleN <= Float.MAX_VALUE) {
                return new DoubleOrFloat((Double) num);
            } else {
                // Simply Double:
                return num;
            }
        } else if (numClass == Float.class) {
            final float floatN = num.floatValue();
            
            // Can we store it in an integer type?
            checkIfWholeNumber: do {
                if ((typeFlags & TypeFlags.MASK_KNOWN_INTEGERS) == 0) break checkIfWholeNumber;
                
                // There's no hope to be 1-precise outside this region. (Although problems can occur even inside it...)
                if (floatN > MAX_FLOAT_OR_INT || floatN < MIN_FLOAT_OR_INT) break checkIfWholeNumber;
                
                int intN = num.intValue();
                double diff = floatN - intN;
                boolean exact;  // We will try to ignore precision glitches (like 0.3 - 0.2 - 0.1 = -2.7E-17)
                if (diff == 0) {
                    exact = true;
                // We already reach ULP 7.6293945E-6 with bytes, so we don't continue with shorts.
                } else if (intN >= Byte.MIN_VALUE && intN <= Byte.MAX_VALUE) {
                    if (diff > 0) {
                        if (diff < 0.00001) {
                            exact = false;
                        } else if (diff > 0.99999) {
                            exact = false;
                            intN++;
                        } else {
                            break checkIfWholeNumber;
                        }
                    } else {  // => diff < 0
                        if (diff > -0.00001) {
                            exact = false;
                        } else if (diff < -0.99999) {
                            exact = false;
                            intN--;
                        } else {
                            break checkIfWholeNumber;
                        }
                    }
                } else {
                    break checkIfWholeNumber;
                }
                
                // If we reach this, it can be treated as a whole number.
                
                if ((typeFlags & TypeFlags.BYTE) != 0 && intN <= Byte.MAX_VALUE && intN >= Byte.MIN_VALUE) {
                    return new FloatOrByte((Float) num, (byte) intN);
                } else if ((typeFlags & TypeFlags.SHORT) != 0 && intN <= Short.MAX_VALUE && intN >= Short.MIN_VALUE) {
                    return new FloatOrShort((Float) num, (short) intN);
                } else if ((typeFlags & TypeFlags.INTEGER) != 0) {
                    return new FloatOrInteger((Float) num, intN);
                } else if ((typeFlags & TypeFlags.LONG) != 0) {
                    // We can't even go outside the range of integers, so we don't need Long variation:
                    return exact
                            ? (Number) new FloatOrInteger((Float) num, intN)
                            : (Number) new FloatOrByte((Float) num, (byte) intN);  // as !exact implies (-128..127)
                }
                // This point is reached if the float value was out of the range of target integer type(s). 
                // Falls through!
            } while (false);
            // If we reach this that means that it can't be treated as a whole number. So it's simply a Float:
            return num;
        } else if (numClass == Byte.class) {
            return num;
        } else if (numClass == Short.class) {
            short pn = num.shortValue(); 
            if ((typeFlags & TypeFlags.BYTE) != 0 && pn <= Byte.MAX_VALUE && pn >= Byte.MIN_VALUE) {
                return new ShortOrByte((Short) num, (byte) pn);
            } else {
                return num;
            }
        } else if (numClass == BigInteger.class) {
            if ((typeFlags
                    & ((TypeFlags.MASK_KNOWN_INTEGERS | TypeFlags.MASK_KNOWN_NONINTEGERS)
                            ^ (TypeFlags.BIG_INTEGER | TypeFlags.BIG_DECIMAL))) != 0) {
                BigInteger biNum = (BigInteger) num;
                final int bitLength = biNum.bitLength();  // Doesn't include sign bit, so it's one less than expected
                if ((typeFlags & TypeFlags.BYTE) != 0 && bitLength <= 7) {
                    return new BigIntegerOrByte(biNum);
                } else if ((typeFlags & TypeFlags.SHORT) != 0 && bitLength <= 15) {
                    return new BigIntegerOrShort(biNum);
                } else if ((typeFlags & TypeFlags.INTEGER) != 0 && bitLength <= 31) {
                    return new BigIntegerOrInteger(biNum);
                } else if ((typeFlags & TypeFlags.LONG) != 0 && bitLength <= 63) {
                    return new BigIntegerOrLong(biNum);
                } else if ((typeFlags & TypeFlags.FLOAT) != 0
                        && (bitLength <= MAX_FLOAT_OR_INT_LOG_2
                            || bitLength == MAX_FLOAT_OR_INT_LOG_2 + 1
                               && biNum.getLowestSetBit() >= MAX_FLOAT_OR_INT_LOG_2)) {
                    return new BigIntegerOrFloat(biNum);
                } else if ((typeFlags & TypeFlags.DOUBLE) != 0
                        && (bitLength <= MAX_DOUBLE_OR_LONG_LOG_2
                            || bitLength == MAX_DOUBLE_OR_LONG_LOG_2 + 1
                               && biNum.getLowestSetBit() >= MAX_DOUBLE_OR_LONG_LOG_2)) {
                    return new BigIntegerOrDouble(biNum);
                } else {
                    return num;
                }
            } else {
                // No relevant coercion target types; return the BigInteger as is:
                return num;
            }
        } else {
            // Unknown number type:
            return num;
        }
    }

    static interface ByteSource { Byte byteValue(); }
    static interface ShortSource { Short shortValue(); }
    static interface IntegerSource { Integer integerValue(); }
    static interface LongSource { Long longValue(); }
    static interface FloatSource { Float floatValue(); }
    static interface DoubleSource { Double doubleValue(); }
    static interface BigIntegerSource { BigInteger bigIntegerValue(); }
    static interface BigDecimalSource { BigDecimal bigDecimalValue(); }
    
    /**
     * Superclass of "Or"-ed numerical types. With an example, a {@code int} 1 has the fallback type {@code byte}, as
     * that's the smallest type that can store the value, so it can be represented as an {@link IntegerOrByte}.
     * This is useful as overloaded method selection only examines the type of the arguments, not the value of them,
     * but with "Or"-ed types we can encode this value-related information into the argument type, hence influencing the
     * method selection.
     */
    abstract static class NumberWithFallbackType extends Number implements Comparable {
        
        protected abstract Number getSourceNumber();

        public int intValue() {
            return getSourceNumber().intValue();
        }

        public long longValue() {
            return getSourceNumber().longValue();
        }

        public float floatValue() {
            return getSourceNumber().floatValue();
        }

        public double doubleValue() {
            return getSourceNumber().doubleValue();
        }

        public byte byteValue() {
            return getSourceNumber().byteValue();
        }

        public short shortValue() {
            return getSourceNumber().shortValue();
        }

        public int hashCode() {
            return getSourceNumber().hashCode();
        }

        public boolean equals(Object obj) {
            if (obj != null && this.getClass() == obj.getClass()) {
                return getSourceNumber().equals(((NumberWithFallbackType) obj).getSourceNumber());
            } else {
                return false;
            }
        }

        public String toString() {
            return getSourceNumber().toString();
        }

        // We have to implement this, so that if a potential matching method expects a Comparable, which is implemented
        // by all the supported numerical types, the "Or" type will be a match. 
        public int compareTo(Object o) {
            Number n = getSourceNumber();
            if (n instanceof Comparable) {
                return ((Comparable) n).compareTo(o); 
            } else {
                throw new ClassCastException(n.getClass().getName() + " is not Comparable.");
            }
        }
        
    }

    /**
     * Holds a {@link BigDecimal} that stores a whole number. When selecting a overloaded method, FreeMarker tries to
     * associate {@link BigDecimal} values to parameters of types that can hold non-whole numbers, unless the
     * {@link BigDecimal} is wrapped into this class, in which case it does the opposite. This mechanism is, however,
     * too rough to prevent roll overs or magnitude losses. Those are not yet handled for backward compatibility (they
     * were suppressed earlier too).
     */
    static final class IntegerBigDecimal extends NumberWithFallbackType {

        private final BigDecimal n;
        
        IntegerBigDecimal(BigDecimal n) {
            this.n = n;
        }

        protected Number getSourceNumber() {
            return n;
        }
        
        public BigInteger bigIntegerValue() {
            return n.toBigInteger();
        }
        
    }

    static abstract class LongOrSmallerInteger extends NumberWithFallbackType {
        
        private final Long n;
        
        protected LongOrSmallerInteger(Long n) {
            this.n = n;
        }

        protected Number getSourceNumber() {
            return n;
        }

        public long longValue() {
            return n.longValue();
        }
        
    }
    
    static class LongOrByte extends LongOrSmallerInteger {
        
        private final byte w; 

        LongOrByte(Long n, byte w) {
            super(n);
            this.w = w;
        }

        public byte byteValue() {
            return w;
        }
        
    }
    
    static class LongOrShort extends LongOrSmallerInteger {
        
        private final short w; 

        LongOrShort(Long n, short w) {
            super(n);
            this.w = w;
        }

        public short shortValue() {
            return w;
        }
        
    }
    
    static class LongOrInteger extends LongOrSmallerInteger {
        
        private final int w; 

        LongOrInteger(Long n, int w) {
            super(n);
            this.w = w;
        }

        public int intValue() {
            return w;
        }
        
    }
    
    static abstract class IntegerOrSmallerInteger extends NumberWithFallbackType {
        
        private final Integer n;
        
        protected IntegerOrSmallerInteger(Integer n) {
            this.n = n;
        }

        protected Number getSourceNumber() {
            return n;
        }

        public int intValue() {
            return n.intValue();
        }
        
    }
    
    static class IntegerOrByte extends IntegerOrSmallerInteger {
        
        private final byte w; 

        IntegerOrByte(Integer n, byte w) {
            super(n);
            this.w = w;
        }

        public byte byteValue() {
            return w;
        }
        
    }
    
    static class IntegerOrShort extends IntegerOrSmallerInteger {
        
        private final short w; 

        IntegerOrShort(Integer n, short w) {
            super(n);
            this.w = w;
        }

        public short shortValue() {
            return w;
        }
        
    }
    
    static class ShortOrByte extends NumberWithFallbackType {
        
        private final Short n;
        private final byte w;
        
        protected ShortOrByte(Short n, byte w) {
            this.n = n;
            this.w = w;
        }

        protected Number getSourceNumber() {
            return n;
        }

        public short shortValue() {
            return n.shortValue();
        }

        public byte byteValue() {
            return w;
        }
        
    }
    
    static abstract class DoubleOrWholeNumber extends NumberWithFallbackType {
        
        private final Double n; 

        protected DoubleOrWholeNumber(Double n) {
            this.n = n;
        }

        protected Number getSourceNumber() {
            return n;
        }
        
        public double doubleValue() {
            return n.doubleValue();
        }
        
    }
    
    static final class DoubleOrByte extends DoubleOrWholeNumber {
        
        private final byte w;

        DoubleOrByte(Double n, byte w) {
            super(n);
            this.w = w;
        }
        
        public byte byteValue() {
            return w;
        }
        
        public short shortValue() {
            return w;
        }
        
        public int intValue() {
            return w;
        }
        
        public long longValue() {
            return w;
        }
        
    }
    
    static final class DoubleOrShort extends DoubleOrWholeNumber {
        
        private final short w;

        DoubleOrShort(Double n, short w) {
            super(n);
            this.w = w;
        }
        
        public short shortValue() {
            return w;
        }
        
        public int intValue() {
            return w;
        }
        
        public long longValue() {
            return w;
        }
        
    }
    
    static final class DoubleOrIntegerOrFloat extends DoubleOrWholeNumber {

        private final int w;

        DoubleOrIntegerOrFloat(Double n, int w) {
            super(n);
            this.w = w;
        }
        
        public int intValue() {
            return w;
        }
        
        public long longValue() {
            return w;
        }
        
    }
    
    static final class DoubleOrInteger extends DoubleOrWholeNumber {

        private final int w;

        DoubleOrInteger(Double n, int w) {
            super(n);
            this.w = w;
        }
        
        public int intValue() {
            return w;
        }
        
        public long longValue() {
            return w;
        }
        
    }
    
    static final class DoubleOrLong extends DoubleOrWholeNumber {

        private final long w;

        DoubleOrLong(Double n, long w) {
            super(n);
            this.w = w;
        }
        
        public long longValue() {
            return w;
        }
        
    }
    
    static final class DoubleOrFloat extends NumberWithFallbackType {
        
        private final Double n;

        DoubleOrFloat(Double n) {
            this.n = n;
        }
        
        public float floatValue() {
            return n.floatValue();
        }
        
        public double doubleValue() {
            return n.doubleValue();
        }

        protected Number getSourceNumber() {
            return n;
        }
        
    }

    static abstract class FloatOrWholeNumber extends NumberWithFallbackType {
        
        private final Float n; 

        FloatOrWholeNumber(Float n) {
            this.n = n;
        }

        protected Number getSourceNumber() {
            return n;
        }
        
        public float floatValue() {
            return n.floatValue();
        }
        
    }
    
    static final class FloatOrByte extends FloatOrWholeNumber {
        
        private final byte w;

        FloatOrByte(Float n, byte w) {
            super(n);
            this.w = w;
        }
        
        public byte byteValue() {
            return w;
        }
        
        public short shortValue() {
            return w;
        }
        
        public int intValue() {
            return w;
        }
        
        public long longValue() {
            return w;
        }
        
    }
    
    static final class FloatOrShort extends FloatOrWholeNumber {
        
        private final short w;

        FloatOrShort(Float n, short w) {
            super(n);
            this.w = w;
        }
        
        public short shortValue() {
            return w;
        }
        
        public int intValue() {
            return w;
        }
        
        public long longValue() {
            return w;
        }
        
    }

    static final class FloatOrInteger extends FloatOrWholeNumber {
        
        private final int w;

        FloatOrInteger(Float n, int w) {
            super(n);
            this.w = w;
        }
        
        public int intValue() {
            return w;
        }
        
        public long longValue() {
            return w;
        }
        
    }

    abstract static class BigIntegerOrPrimitive extends NumberWithFallbackType {

        protected final BigInteger n;
        
        BigIntegerOrPrimitive(BigInteger n) {
            this.n = n;
        }

        protected Number getSourceNumber() {
            return n;
        }
        
    }
    
    final static class BigIntegerOrByte extends BigIntegerOrPrimitive {

        BigIntegerOrByte(BigInteger n) {
            super(n);
        }

    }
    
    final static class BigIntegerOrShort extends BigIntegerOrPrimitive {

        BigIntegerOrShort(BigInteger n) {
            super(n);
        }

    }
    
    final static class BigIntegerOrInteger extends BigIntegerOrPrimitive {

        BigIntegerOrInteger(BigInteger n) {
            super(n);
        }

    }
    
    final static class BigIntegerOrLong extends BigIntegerOrPrimitive {

        BigIntegerOrLong(BigInteger n) {
            super(n);
        }

    }

    abstract static class BigIntegerOrFPPrimitive extends BigIntegerOrPrimitive {

        BigIntegerOrFPPrimitive(BigInteger n) {
            super(n);
        }

        /** Faster version of {@link BigDecimal#floatValue()}, utilizes that the number known to fit into a long. */
        public float floatValue() {
            return n.longValue(); 
        }
        
        /** Faster version of {@link BigDecimal#doubleValue()}, utilizes that the number known to fit into a long. */
        public double doubleValue() {
            return n.longValue(); 
        }

    }
    
    final static class BigIntegerOrFloat extends BigIntegerOrFPPrimitive {

        BigIntegerOrFloat(BigInteger n) {
            super(n);
        }

    }
    
    final static class BigIntegerOrDouble extends BigIntegerOrFPPrimitive {

        BigIntegerOrDouble(BigInteger n) {
            super(n);
        }
        
    }
    
    /**
     * Returns a non-negative number that indicates how much we want to avoid a given numerical type conversion. Since
     * we only consider the types here, not the actual value, we always consider the worst case scenario. Like it will
     * say that converting int to short is not allowed, although int 1 can be converted to byte without loss. To account
     * for such situations, "Or"-ed types, like {@link IntegerOrByte} has to be used. 
     * 
     * @param fromC the non-primitive type of the argument (with other words, the actual type).
     *        Must be {@link Number} or its subclass. This is possibly an {@link NumberWithFallbackType} subclass.
     * @param toC the <em>non-primitive</em> type of the target parameter (with other words, the format type).
     *        Must be a {@link Number} subclass, not {@link Number} itself.
     *        Must <em>not</em> be {@link NumberWithFallbackType} or its subclass.
     * 
     * @return
     *     <p>The possible values are:
     *     <ul>
     *       <li>0: No conversion is needed
     *       <li>[0, 30000): Lossless conversion
     *       <li>[30000, 40000): Smaller precision loss in mantissa is possible.
     *       <li>[40000, 50000): Bigger precision loss in mantissa is possible.
     *       <li>{@link Integer#MAX_VALUE}: Conversion not allowed due to the possibility of magnitude loss or
     *          overflow</li>
     *     </ul>
     * 
     *     <p>At some places, we only care if the conversion is possible, i.e., whether the return value is
     *     {@link Integer#MAX_VALUE} or not. But when multiple overloaded methods have an argument type to which we
     *     could convert to, this number will influence which of those will be chosen.
     */
    static int getArgumentConversionPrice(Class fromC, Class toC) {
        // DO NOT EDIT, generated code!
        // See: src\main\misc\overloadedNumberRules\README.txt
        if (toC == fromC) {
            return 0;
        } else if (toC == Integer.class) {
            if (fromC == IntegerBigDecimal.class) return 31003;
            else if (fromC == BigDecimal.class) return 41003;
            else if (fromC == Long.class) return Integer.MAX_VALUE;
            else if (fromC == Double.class) return Integer.MAX_VALUE;
            else if (fromC == Float.class) return Integer.MAX_VALUE;
            else if (fromC == Byte.class) return 10003;
            else if (fromC == BigInteger.class) return Integer.MAX_VALUE;
            else if (fromC == LongOrInteger.class) return 21003;
            else if (fromC == DoubleOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrIntegerOrFloat.class) return 22003;
            else if (fromC == DoubleOrInteger.class) return 22003;
            else if (fromC == DoubleOrLong.class) return Integer.MAX_VALUE;
            else if (fromC == IntegerOrByte.class) return 0;
            else if (fromC == DoubleOrByte.class) return 22003;
            else if (fromC == LongOrByte.class) return 21003;
            else if (fromC == Short.class) return 10003;
            else if (fromC == LongOrShort.class) return 21003;
            else if (fromC == ShortOrByte.class) return 10003;
            else if (fromC == FloatOrInteger.class) return 21003;
            else if (fromC == FloatOrByte.class) return 21003;
            else if (fromC == FloatOrShort.class) return 21003;
            else if (fromC == BigIntegerOrInteger.class) return 16003;
            else if (fromC == BigIntegerOrLong.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrDouble.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrByte.class) return 16003;
            else if (fromC == IntegerOrShort.class) return 0;
            else if (fromC == DoubleOrShort.class) return 22003;
            else if (fromC == BigIntegerOrShort.class) return 16003;
            else return Integer.MAX_VALUE;
        } else if (toC == Long.class) {
            if (fromC == Integer.class) return 10004;
            else if (fromC == IntegerBigDecimal.class) return 31004;
            else if (fromC == BigDecimal.class) return 41004;
            else if (fromC == Double.class) return Integer.MAX_VALUE;
            else if (fromC == Float.class) return Integer.MAX_VALUE;
            else if (fromC == Byte.class) return 10004;
            else if (fromC == BigInteger.class) return Integer.MAX_VALUE;
            else if (fromC == LongOrInteger.class) return 0;
            else if (fromC == DoubleOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrIntegerOrFloat.class) return 21004;
            else if (fromC == DoubleOrInteger.class) return 21004;
            else if (fromC == DoubleOrLong.class) return 21004;
            else if (fromC == IntegerOrByte.class) return 10004;
            else if (fromC == DoubleOrByte.class) return 21004;
            else if (fromC == LongOrByte.class) return 0;
            else if (fromC == Short.class) return 10004;
            else if (fromC == LongOrShort.class) return 0;
            else if (fromC == ShortOrByte.class) return 10004;
            else if (fromC == FloatOrInteger.class) return 21004;
            else if (fromC == FloatOrByte.class) return 21004;
            else if (fromC == FloatOrShort.class) return 21004;
            else if (fromC == BigIntegerOrInteger.class) return 15004;
            else if (fromC == BigIntegerOrLong.class) return 15004;
            else if (fromC == BigIntegerOrDouble.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrByte.class) return 15004;
            else if (fromC == IntegerOrShort.class) return 10004;
            else if (fromC == DoubleOrShort.class) return 21004;
            else if (fromC == BigIntegerOrShort.class) return 15004;
            else return Integer.MAX_VALUE;
        } else if (toC == Double.class) {
            if (fromC == Integer.class) return 20007;
            else if (fromC == IntegerBigDecimal.class) return 32007;
            else if (fromC == BigDecimal.class) return 32007;
            else if (fromC == Long.class) return 30007;
            else if (fromC == Float.class) return 10007;
            else if (fromC == Byte.class) return 20007;
            else if (fromC == BigInteger.class) return Integer.MAX_VALUE;
            else if (fromC == LongOrInteger.class) return 21007;
            else if (fromC == DoubleOrFloat.class) return 0;
            else if (fromC == DoubleOrIntegerOrFloat.class) return 0;
            else if (fromC == DoubleOrInteger.class) return 0;
            else if (fromC == DoubleOrLong.class) return 0;
            else if (fromC == IntegerOrByte.class) return 20007;
            else if (fromC == DoubleOrByte.class) return 0;
            else if (fromC == LongOrByte.class) return 21007;
            else if (fromC == Short.class) return 20007;
            else if (fromC == LongOrShort.class) return 21007;
            else if (fromC == ShortOrByte.class) return 20007;
            else if (fromC == FloatOrInteger.class) return 10007;
            else if (fromC == FloatOrByte.class) return 10007;
            else if (fromC == FloatOrShort.class) return 10007;
            else if (fromC == BigIntegerOrInteger.class) return 20007;
            else if (fromC == BigIntegerOrLong.class) return 30007;
            else if (fromC == BigIntegerOrDouble.class) return 20007;
            else if (fromC == BigIntegerOrFloat.class) return 20007;
            else if (fromC == BigIntegerOrByte.class) return 20007;
            else if (fromC == IntegerOrShort.class) return 20007;
            else if (fromC == DoubleOrShort.class) return 0;
            else if (fromC == BigIntegerOrShort.class) return 20007;
            else return Integer.MAX_VALUE;
        } else if (toC == Float.class) {
            if (fromC == Integer.class) return 30006;
            else if (fromC == IntegerBigDecimal.class) return 33006;
            else if (fromC == BigDecimal.class) return 33006;
            else if (fromC == Long.class) return 40006;
            else if (fromC == Double.class) return Integer.MAX_VALUE;
            else if (fromC == Byte.class) return 20006;
            else if (fromC == BigInteger.class) return Integer.MAX_VALUE;
            else if (fromC == LongOrInteger.class) return 30006;
            else if (fromC == DoubleOrFloat.class) return 30006;
            else if (fromC == DoubleOrIntegerOrFloat.class) return 23006;
            else if (fromC == DoubleOrInteger.class) return 30006;
            else if (fromC == DoubleOrLong.class) return 40006;
            else if (fromC == IntegerOrByte.class) return 24006;
            else if (fromC == DoubleOrByte.class) return 23006;
            else if (fromC == LongOrByte.class) return 24006;
            else if (fromC == Short.class) return 20006;
            else if (fromC == LongOrShort.class) return 24006;
            else if (fromC == ShortOrByte.class) return 20006;
            else if (fromC == FloatOrInteger.class) return 0;
            else if (fromC == FloatOrByte.class) return 0;
            else if (fromC == FloatOrShort.class) return 0;
            else if (fromC == BigIntegerOrInteger.class) return 30006;
            else if (fromC == BigIntegerOrLong.class) return 40006;
            else if (fromC == BigIntegerOrDouble.class) return 40006;
            else if (fromC == BigIntegerOrFloat.class) return 24006;
            else if (fromC == BigIntegerOrByte.class) return 24006;
            else if (fromC == IntegerOrShort.class) return 24006;
            else if (fromC == DoubleOrShort.class) return 23006;
            else if (fromC == BigIntegerOrShort.class) return 24006;
            else return Integer.MAX_VALUE;
        } else if (toC == Byte.class) {
            if (fromC == Integer.class) return Integer.MAX_VALUE;
            else if (fromC == IntegerBigDecimal.class) return 35001;
            else if (fromC == BigDecimal.class) return 45001;
            else if (fromC == Long.class) return Integer.MAX_VALUE;
            else if (fromC == Double.class) return Integer.MAX_VALUE;
            else if (fromC == Float.class) return Integer.MAX_VALUE;
            else if (fromC == BigInteger.class) return Integer.MAX_VALUE;
            else if (fromC == LongOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrIntegerOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrLong.class) return Integer.MAX_VALUE;
            else if (fromC == IntegerOrByte.class) return 22001;
            else if (fromC == DoubleOrByte.class) return 25001;
            else if (fromC == LongOrByte.class) return 23001;
            else if (fromC == Short.class) return Integer.MAX_VALUE;
            else if (fromC == LongOrShort.class) return Integer.MAX_VALUE;
            else if (fromC == ShortOrByte.class) return 21001;
            else if (fromC == FloatOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == FloatOrByte.class) return 23001;
            else if (fromC == FloatOrShort.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrLong.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrDouble.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrByte.class) return 18001;
            else if (fromC == IntegerOrShort.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrShort.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrShort.class) return Integer.MAX_VALUE;
            else return Integer.MAX_VALUE;
        } else if (toC == Short.class) {
            if (fromC == Integer.class) return Integer.MAX_VALUE;
            else if (fromC == IntegerBigDecimal.class) return 34002;
            else if (fromC == BigDecimal.class) return 44002;
            else if (fromC == Long.class) return Integer.MAX_VALUE;
            else if (fromC == Double.class) return Integer.MAX_VALUE;
            else if (fromC == Float.class) return Integer.MAX_VALUE;
            else if (fromC == Byte.class) return 10002;
            else if (fromC == BigInteger.class) return Integer.MAX_VALUE;
            else if (fromC == LongOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrIntegerOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrLong.class) return Integer.MAX_VALUE;
            else if (fromC == IntegerOrByte.class) return 21002;
            else if (fromC == DoubleOrByte.class) return 24002;
            else if (fromC == LongOrByte.class) return 22002;
            else if (fromC == LongOrShort.class) return 22002;
            else if (fromC == ShortOrByte.class) return 0;
            else if (fromC == FloatOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == FloatOrByte.class) return 22002;
            else if (fromC == FloatOrShort.class) return 22002;
            else if (fromC == BigIntegerOrInteger.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrLong.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrDouble.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == BigIntegerOrByte.class) return 17002;
            else if (fromC == IntegerOrShort.class) return 21002;
            else if (fromC == DoubleOrShort.class) return 24002;
            else if (fromC == BigIntegerOrShort.class) return 17002;
            else return Integer.MAX_VALUE;
        } else if (toC == BigDecimal.class) {
            if (fromC == Integer.class) return 20008;
            else if (fromC == IntegerBigDecimal.class) return 0;
            else if (fromC == Long.class) return 20008;
            else if (fromC == Double.class) return 20008;
            else if (fromC == Float.class) return 20008;
            else if (fromC == Byte.class) return 20008;
            else if (fromC == BigInteger.class) return 10008;
            else if (fromC == LongOrInteger.class) return 20008;
            else if (fromC == DoubleOrFloat.class) return 20008;
            else if (fromC == DoubleOrIntegerOrFloat.class) return 20008;
            else if (fromC == DoubleOrInteger.class) return 20008;
            else if (fromC == DoubleOrLong.class) return 20008;
            else if (fromC == IntegerOrByte.class) return 20008;
            else if (fromC == DoubleOrByte.class) return 20008;
            else if (fromC == LongOrByte.class) return 20008;
            else if (fromC == Short.class) return 20008;
            else if (fromC == LongOrShort.class) return 20008;
            else if (fromC == ShortOrByte.class) return 20008;
            else if (fromC == FloatOrInteger.class) return 20008;
            else if (fromC == FloatOrByte.class) return 20008;
            else if (fromC == FloatOrShort.class) return 20008;
            else if (fromC == BigIntegerOrInteger.class) return 10008;
            else if (fromC == BigIntegerOrLong.class) return 10008;
            else if (fromC == BigIntegerOrDouble.class) return 10008;
            else if (fromC == BigIntegerOrFloat.class) return 10008;
            else if (fromC == BigIntegerOrByte.class) return 10008;
            else if (fromC == IntegerOrShort.class) return 20008;
            else if (fromC == DoubleOrShort.class) return 20008;
            else if (fromC == BigIntegerOrShort.class) return 10008;
            else return Integer.MAX_VALUE;
        } else if (toC == BigInteger.class) {
            if (fromC == Integer.class) return 10005;
            else if (fromC == IntegerBigDecimal.class) return 10005;
            else if (fromC == BigDecimal.class) return 40005;
            else if (fromC == Long.class) return 10005;
            else if (fromC == Double.class) return Integer.MAX_VALUE;
            else if (fromC == Float.class) return Integer.MAX_VALUE;
            else if (fromC == Byte.class) return 10005;
            else if (fromC == LongOrInteger.class) return 10005;
            else if (fromC == DoubleOrFloat.class) return Integer.MAX_VALUE;
            else if (fromC == DoubleOrIntegerOrFloat.class) return 21005;
            else if (fromC == DoubleOrInteger.class) return 21005;
            else if (fromC == DoubleOrLong.class) return 21005;
            else if (fromC == IntegerOrByte.class) return 10005;
            else if (fromC == DoubleOrByte.class) return 21005;
            else if (fromC == LongOrByte.class) return 10005;
            else if (fromC == Short.class) return 10005;
            else if (fromC == LongOrShort.class) return 10005;
            else if (fromC == ShortOrByte.class) return 10005;
            else if (fromC == FloatOrInteger.class) return 25005;
            else if (fromC == FloatOrByte.class) return 25005;
            else if (fromC == FloatOrShort.class) return 25005;
            else if (fromC == BigIntegerOrInteger.class) return 0;
            else if (fromC == BigIntegerOrLong.class) return 0;
            else if (fromC == BigIntegerOrDouble.class) return 0;
            else if (fromC == BigIntegerOrFloat.class) return 0;
            else if (fromC == BigIntegerOrByte.class) return 0;
            else if (fromC == IntegerOrShort.class) return 10005;
            else if (fromC == DoubleOrShort.class) return 21005;
            else if (fromC == BigIntegerOrShort.class) return 0;
            else return Integer.MAX_VALUE;
        } else {
            // Unknown toC; we don't know how to convert to it:
            return Integer.MAX_VALUE;
        }        
    }

    static int compareNumberTypeSpecificity(Class c1, Class c2) {
        // DO NOT EDIT, generated code!
        // See: src\main\misc\overloadedNumberRules\README.txt
        c1 = ClassUtil.primitiveClassToBoxingClass(c1);
        c2 = ClassUtil.primitiveClassToBoxingClass(c2);
        
        if (c1 == c2) return 0;
        
        if (c1 == Integer.class) {
            if (c2 == Long.class) return 4 - 3;
            if (c2 == Double.class) return 7 - 3;
            if (c2 == Float.class) return 6 - 3;
            if (c2 == Byte.class) return 1 - 3;
            if (c2 == Short.class) return 2 - 3;
            if (c2 == BigDecimal.class) return 8 - 3;
            if (c2 == BigInteger.class) return 5 - 3;
            return 0;
        }
        if (c1 == Long.class) {
            if (c2 == Integer.class) return 3 - 4;
            if (c2 == Double.class) return 7 - 4;
            if (c2 == Float.class) return 6 - 4;
            if (c2 == Byte.class) return 1 - 4;
            if (c2 == Short.class) return 2 - 4;
            if (c2 == BigDecimal.class) return 8 - 4;
            if (c2 == BigInteger.class) return 5 - 4;
            return 0;
        }
        if (c1 == Double.class) {
            if (c2 == Integer.class) return 3 - 7;
            if (c2 == Long.class) return 4 - 7;
            if (c2 == Float.class) return 6 - 7;
            if (c2 == Byte.class) return 1 - 7;
            if (c2 == Short.class) return 2 - 7;
            if (c2 == BigDecimal.class) return 8 - 7;
            if (c2 == BigInteger.class) return 5 - 7;
            return 0;
        }
        if (c1 == Float.class) {
            if (c2 == Integer.class) return 3 - 6;
            if (c2 == Long.class) return 4 - 6;
            if (c2 == Double.class) return 7 - 6;
            if (c2 == Byte.class) return 1 - 6;
            if (c2 == Short.class) return 2 - 6;
            if (c2 == BigDecimal.class) return 8 - 6;
            if (c2 == BigInteger.class) return 5 - 6;
            return 0;
        }
        if (c1 == Byte.class) {
            if (c2 == Integer.class) return 3 - 1;
            if (c2 == Long.class) return 4 - 1;
            if (c2 == Double.class) return 7 - 1;
            if (c2 == Float.class) return 6 - 1;
            if (c2 == Short.class) return 2 - 1;
            if (c2 == BigDecimal.class) return 8 - 1;
            if (c2 == BigInteger.class) return 5 - 1;
            return 0;
        }
        if (c1 == Short.class) {
            if (c2 == Integer.class) return 3 - 2;
            if (c2 == Long.class) return 4 - 2;
            if (c2 == Double.class) return 7 - 2;
            if (c2 == Float.class) return 6 - 2;
            if (c2 == Byte.class) return 1 - 2;
            if (c2 == BigDecimal.class) return 8 - 2;
            if (c2 == BigInteger.class) return 5 - 2;
            return 0;
        }
        if (c1 == BigDecimal.class) {
            if (c2 == Integer.class) return 3 - 8;
            if (c2 == Long.class) return 4 - 8;
            if (c2 == Double.class) return 7 - 8;
            if (c2 == Float.class) return 6 - 8;
            if (c2 == Byte.class) return 1 - 8;
            if (c2 == Short.class) return 2 - 8;
            if (c2 == BigInteger.class) return 5 - 8;
            return 0;
        }
        if (c1 == BigInteger.class) {
            if (c2 == Integer.class) return 3 - 5;
            if (c2 == Long.class) return 4 - 5;
            if (c2 == Double.class) return 7 - 5;
            if (c2 == Float.class) return 6 - 5;
            if (c2 == Byte.class) return 1 - 5;
            if (c2 == Short.class) return 2 - 5;
            if (c2 == BigDecimal.class) return 8 - 5;
            return 0;
        }
        return 0;
    }

}
