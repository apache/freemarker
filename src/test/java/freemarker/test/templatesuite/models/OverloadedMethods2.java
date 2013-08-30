package freemarker.test.templatesuite.models;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import freemarker.ext.beans.RationalNumber;
import freemarker.template.utility.StringUtil;

public class OverloadedMethods2 {

    public String mVarargs(String... a1) {
        StringBuilder sb = new StringBuilder();
        for (String s : a1) {
            sb.append(s);
        }
        return "mVarargs(String... a1 = " + sb + ")";
    }
    
    public BigInteger bigInteger(BigDecimal n) {
        return n.toBigInteger();
    }

    public RationalNumber rational(int a, int b) {
        return new RationalNumber(a, b);
    }
    
    public String mVarargs(File a1, String... a2) {
        return "mVarargs(File a1, String... a2)";
    }

    public NumberAndStringModel getNnS(String s) {
        return new NumberAndStringModel(s);
    }
    
    public String mNull1(String a1) {
        return "mNull1(String a1 = " + a1 + ")";
    }

    public String mNull1(int a1) {
        return "mNull1(int a1 = " + a1 + ")";
    }
    
    public String mNull2(String a1) {
        return "mNull2(String a1 = " + a1 + ")";
    }
    
    public String mNull2(Object a1) {
        return "mNull2(Object a1 = " + a1 + ")";
    }
    
    public String mSpecificity(Object a1, String a2) {
        return "mSpecificity(Object a1, String a2)";
    }
    
    public String mSpecificity(String a1, Object a2) {
        return "mSpecificity(String a1, Object a2)";
    }
    
    public String mChar(char a1) {
        return "mChar(char a1 = " + a1 + ")";
    }
    
    public String mChar(Character a1) {
        return "mChar(Character a1 = " + a1 + ")";
    }
    
    public String mBoolean(boolean a1) {
        return "mBoolean(boolean a1 = " + a1 + ")";
    }
    
    public String mBoolean(Boolean a1) {
        return "mBoolean(Boolean a1 = " + a1 + ")";
    }

    public int mIntNonOverloaded(int a1) {
        return a1;
    }

    public String mIntPrimVSBoxed(int a1) {
        return "mIntPrimVSBoxed(int a1 = " + a1 + ")";
    }
    
    public String mIntPrimVSBoxed(Integer a1) {
        return "mIntPrimVSBoxed(Integer a1 = " + a1 + ")";
    }

    public String mNumPrimVSPrim(short a1) {
        return "mNumPrimVSPrim(short a1 = " + a1 + ")";
    }
    
    public String mNumPrimVSPrim(long a1) {
        return "mNumPrimVSPrim(long a1 = " + a1 + ")";
    }

    public String mNumBoxedVSBoxed(Short a1) {
        return "mNumBoxedVSBoxed(Short a1 = " + a1 + ")";
    }
    
    public String mNumBoxedVSBoxed(Long a1) {
        return "mNumBoxedVSBoxed(Long a1 = " + a1 + ")";
    }

    public String mNumUnambigous(Short a1, boolean otherOverload) {
        return "mmNumUnambigous won't be called";
    }
    
    public String mNumUnambigous(Integer a1) {
        return "mNumUnambigous(Integer a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll(Byte a1) {
        return "mNumBoxedAll(Byte a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll(Short a1) {
        return "mNumBoxedAll(Short a1 = " + a1 + ")";
    }

    public String mNumBoxedAll(Integer a1) {
        return "mNumBoxedAll(Integer a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll(Long a1) {
        return "mNumBoxedAll(Long a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll(Float a1) {
        return "mNumBoxedAll(Float a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll(Double a1) {
        return "mNumBoxedAll(Double a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll(BigInteger a1) {
        return "mNumBoxedAll(BigInteger a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll(BigDecimal a1) {
        return "mNumBoxedAll(BigDecimal a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll(byte a1) {
        return "mNumPrimAll(byte a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll(short a1) {
        return "mNumPrimAll(short a1 = " + a1 + ")";
    }

    public String mNumPrimAll(int a1) {
        return "mNumPrimAll(int a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll(long a1) {
        return "mNumPrimAll(long a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll(float a1) {
        return "mNumPrimAll(float a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll(double a1) {
        return "mNumPrimAll(double a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll(BigInteger a1) {
        return "mNumPrimAll(BigInteger a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll(BigDecimal a1) {
        return "mNumPrimAll(BigDecimal a1 = " + a1 + ")";
    }

    
    public String mNumBoxedAll2nd(Short a1) {
        return "mNumBoxedAll2nd(Short a1 = " + a1 + ")";
    }

    public String mNumBoxedAll2nd(Long a1) {
        return "mNumBoxedAll2nd(Long a1 = " + a1 + ")";
    }
    
    public String mNumBoxedAll2nd(Double a1) {
        return "mNumBoxedAll2nd(Double a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll2nd(short a1) {
        return "mNumPrimAll2nd(short a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll2nd(long a1) {
        return "mNumPrimAll2nd(long a1 = " + a1 + ")";
    }
    
    public String mNumPrimAll2nd(double a1) {
        return "mNumPrimAll2nd(double a1 = " + a1 + ")";
    }
    
    public String mNumPrimFallbackToNumber(long a1) {
        return "mNumPrimFallbackToNumber(long a1 = " + a1 + ")";
    }
    
    public String mNumPrimFallbackToNumber(Number a1) {
        return "mNumPrimFallbackToNumber(Number a1 = " + a1 + ")";
    }
    
    public String mNumPrimFallbackToNumber(Object a1) {
        return "mNumPrimFallbackToNumber(Object a1 = " + a1 + ")";
    }
    
    public String mNumBoxedFallbackToNumber(Long a1) {
        return "mNumBoxedFallbackToNumber(Long a1 = " + a1 + ")";
    }
    
    public String mNumBoxedFallbackToNumber(Number a1) {
        return "mNumBoxedFallbackToNumber(Number a1 = " + a1 + ")";
    }
    
    public String mNumBoxedFallbackToNumber(Object a1) {
        return "mNumBoxedFallbackToNumber(Object a1 = " + a1 + ")";
    }

    public String mDecimalLoss(int a1) {
        return "mDecimalLoss(int a1 = " + a1 + ")";
    }
        
    public String mDecimalLoss(double a1) {
        return "mDecimalLoss(double a1 = " + a1 + ")";
    }
    
    public String mNumConversionLoses1(byte i, Object o1, Object o2) {
        return "byte " + i;
    }
    
    public String mNumConversionLoses1(double i, Object o1, Object o2) {
        return "double " + i;
    }

    public String mNumConversionLoses1(Number i, String o1, String o2) {
        return "Number " + i + " " + i.getClass().getName();
    }

    public String mNumConversionLoses2(int i, Object o1, Object o2) {
        return "int " + i;
    }

    public String mNumConversionLoses2(long i, Object o1, Object o2) {
        return "long " + i;
    }

    public String mNumConversionLoses2(Number i, String o1, String o2) {
        return "Number " + i + " " + i.getClass().getName();
    }

    public String mNumConversionLoses3(int i, Object o1, Object o2) {
        return "int " + i;
    }

    public String mNumConversionLoses3(Serializable i, String o1, String o2) {
        return "Serializable " + i + " " + i.getClass().getName();
    }
    
    public String nIntAndLong(int i) {
        return "nIntAndLong(int " + i + ")";
    }
    
    public String nIntAndLong(long i) {
        return "nIntAndLong(long " + i + ")";
    }

    public String nIntAndShort(int i) {
        return "nIntAndShort(int " + i + ")";
    }
    
    public String nIntAndShort(short i) {
        return "nIntAndShort(short " + i + ")";
    }

    public String nLongAndShort(long i) {
        return "nLongAndShort(long " + i + ")";
    }
    
    public String nLongAndShort(short i) {
        return "nLongAndShort(short " + i + ")";
    }

    public String varargs1(String s, int... xs) {
        return "varargs1(String s = " + StringUtil.jQuote(s) + ", int... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs1(String s, double... xs) {
        return "varargs1(String s = " + StringUtil.jQuote(s) + ", double... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs1(String s, Object... xs) {
        return "varargs1(String s = " + StringUtil.jQuote(s) + ", Object... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs1(Object s, Object... xs) {
        return "varargs1(Object s = " + s + ", Object... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs2(int... xs) {
        return "varargs2(int... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs2(double... xs) {
        return "varargs2(double... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs3(String... xs) {
        return "varargs3(String... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs3(Comparable... xs) {
        return "varargs3(Comparable... xs = [" + arrayToString(xs) + "])";
    }
    
    public String varargs3(Object... xs) {
        return "varargs3(Object... xs = [" + arrayToString(xs) + "])";
    }
    
    public String varargs4(Integer... xs) {
        return "varargs4(Integer... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs4(int... xs) {
        return "varargs4(int... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs5(int... xs) {
        return "varargs5(int... xs = [" + arrayToString(xs) + "])";
    }
    
    public String varargs5(int a1, int... xs) {
        return "varargs5(int a1 = " + a1 + ", int... xs = [" + arrayToString(xs) + "])";
    }
    
    public String varargs5(int a1, int a2, int... xs) {
        return "varargs5(int a1 = " + a1 + ", int a2 = " + a2 + ", int... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs5(int a1, int a2, int a3, int... xs) {
        return "varargs5(int a1 = " + a1 + ", int a2 = " + a2 + ", int a3 = " + a3
                + ", int... xs = [" + arrayToString(xs) + "])";
    }

    public String varargs6(String a1, int... xs) {
        return "varargs6(String a1 = " + a1 + ", int... xs = [" + arrayToString(xs) + "])";
    }
    
    public String varargs6(Object a1, int a2, int... xs) {
        return "varargs6(Object a1 = " + a1 + ", int a2 = " + a2 + ", int... xs = [" + arrayToString(xs) + "])";
    }
    
    public String varargs7(int... xs) {
        return "varargs7(int... xs = [" + arrayToString(xs) + "])";
    }
    
    public String varargs7(short a1, int... xs) {
        return "varargs7(short a1 = " + a1 + ", int... xs = [" + arrayToString(xs) + "])";
    }
    
    private String arrayToString(int[] xs) {
        StringBuilder sb = new StringBuilder();
        for (int x : xs) {
            if (sb.length() != 0) sb.append(", ");
            sb.append(x);
        }
        return sb.toString();
    }

    private String arrayToString(double[] xs) {
        StringBuilder sb = new StringBuilder();
        for (double x : xs) {
            if (sb.length() != 0) sb.append(", ");
            sb.append(x);
        }
        return sb.toString();
    }

    private String arrayToString(Object[] xs) {
        StringBuilder sb = new StringBuilder();
        for (Object x : xs) {
            if (sb.length() != 0) sb.append(", ");
            sb.append(x);
        }
        return sb.toString();
    }
    
}
