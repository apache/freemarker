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

package freemarker.test.templatesuite.models;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.core.Environment;
import freemarker.ext.beans.RationalNumber;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.StringUtil;
import freemarker.test.utility.Helpers;

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
        return "varargs1(String s = " + StringUtil.jQuote(s) + ", int... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs1(String s, double... xs) {
        return "varargs1(String s = " + StringUtil.jQuote(s) + ", double... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs1(String s, Object... xs) {
        return "varargs1(String s = " + StringUtil.jQuote(s) + ", Object... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs1(Object s, Object... xs) {
        return "varargs1(Object s = " + s + ", Object... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs2(int... xs) {
        return "varargs2(int... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs2(double... xs) {
        return "varargs2(double... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs3(String... xs) {
        return "varargs3(String... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs3(Comparable... xs) {
        return "varargs3(Comparable... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String varargs3(Object... xs) {
        return "varargs3(Object... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String varargs4(Integer... xs) {
        return "varargs4(Integer... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs4(int... xs) {
        return "varargs4(int... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs5(int... xs) {
        return "varargs5(int... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String varargs5(int a1, int... xs) {
        return "varargs5(int a1 = " + a1 + ", int... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String varargs5(int a1, int a2, int... xs) {
        return "varargs5(int a1 = " + a1 + ", int a2 = " + a2 + ", int... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs5(int a1, int a2, int a3, int... xs) {
        return "varargs5(int a1 = " + a1 + ", int a2 = " + a2 + ", int a3 = " + a3
                + ", int... xs = " + Helpers.arrayToString(xs) + ")";
    }

    public String varargs6(String a1, int... xs) {
        return "varargs6(String a1 = " + a1 + ", int... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String varargs6(Object a1, int a2, int... xs) {
        return "varargs6(Object a1 = " + a1 + ", int a2 = " + a2 + ", int... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String varargs7(int... xs) {
        return "varargs7(int... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String varargs7(short a1, int... xs) {
        return "varargs7(short a1 = " + a1 + ", int... xs = " + Helpers.arrayToString(xs) + ")";
    }
    
    public String mNullAmbiguous(String s) {
        return "mNullAmbiguous(String s = " + s + ")";
    }

    public String mNullAmbiguous(int i) {
        return "mNullAmbiguous(int i = " + i + ")";
    }

    public String mNullAmbiguous(File f) {
        return "mNullAmbiguous(File f = " + f + ")";
    }
    
    public String mNullAmbiguous2(String s) {
        return "mNullNonAmbiguous(String s = " + s + ")";
    }

    public String mNullAmbiguous2(File f) {
        return "mNullAmbiguous(File f = " + f + ")";
    }

    public String mNullAmbiguous2(Object o) {
        return "mNullAmbiguous(Object o = " + o + ")";
    }

    public String mNullNonAmbiguous(String s) {
        return "mNullNonAmbiguous(String s = " + s + ")";
    }

    public String mNullNonAmbiguous(int i) {
        return "mNullNonAmbiguous(int i = " + i + ")";
    }
    
    public String mVarargsIgnoredTail(int i, double... ds) {
        return "mVarargsIgnoredTail(int i = " + i + ", double... ds = " + Helpers.arrayToString(ds) + ")"; 
    }
    
    public String mVarargsIgnoredTail(int... is) {
        return "mVarargsIgnoredTail(int... is = " + Helpers.arrayToString(is) + ")"; 
    }
    
    public String mLowRankWins(int x, int y, Object o) {
        return "mLowRankWins(int x = " + x + ", int y = " + y + ", Object o = " + o + ")";
    }

    public String mLowRankWins(Integer x, Integer y, String s) {
        return "mLowRankWins(Integer x = " + x + ", Integer y = " + y + ", String s = " + s + ")";
    }
    
    public String mRareWrappings(File f, double d1, Double d2, double d3, boolean b) {
        return "mRareWrappings(File f = " + f + ", double d1 = " + d1 + ", Double d2 = " + d2
                + ", double d3 = " + d3 + ", b = " + b + ")";
    }

    public String mRareWrappings(Object o, double d1, Double d2, Double d3, boolean b) {
        return "mRareWrappings(Object o = " + o + ", double d1 = " + d1 + ", Double d2 = " + d2
                + ", double d3 = " + d3 + ", b = " + b + ")";
    }

    public String mRareWrappings(String s, double d1, Double d2, Double d3, boolean b) {
        return "mRareWrappings(String s = " + s + ", double d1 = " + d1 + ", Double d2 = " + d2
                + ", double d3 = " + d3 + ", b = " + b + ")";
    }

    public String mRareWrappings2(String s) {
        return "mRareWrappings2(String s = " + s + ")";
    }
    
    public String mRareWrappings2(byte b) {
        return "mRareWrappings2(byte b = " + b + ")";
    }
    
    public File getFile() {
        return new File("file");
    }

    public String mSeqToArrayNonOverloaded(String[] items, String s) {
        return "mSeqToArrayNonOverloaded(String[] " + Helpers.arrayToString(items) + ", String " + s + ")";
    }
    
    public String mSeqToArrayGoodHint(String[] items, String s) {
        return "mSeqToArrayGoodHint(String[] " + Helpers.arrayToString(items) + ", String " + s + ")";
    }

    public String mSeqToArrayGoodHint(String[] items, int i) {
        return "mSeqToArrayGoodHint(String[] " + Helpers.arrayToString(items) + ", int " + i + ")";
    }

    public String mSeqToArrayGoodHint2(String[] items, String s) {
        return "mSeqToArrayGoodHint2(String[] " + Helpers.arrayToString(items) + ", String " + s + ")";
    }

    public String mSeqToArrayGoodHint2(String item) {
        return "mSeqToArrayGoodHint2(String " + item + ")";
    }
    
    public String mSeqToArrayPoorHint(String[] items, String s) {
        return "mSeqToArrayPoorHint(String[] " + Helpers.arrayToString(items) + ", String " + s + ")";
    }

    public String mSeqToArrayPoorHint(String item, int i) {
        return "mSeqToArrayPoorHint(String " + item + ", int " + i + ")";
    }

    public String mSeqToArrayPoorHint2(String[] items) {
        return "mSeqToArrayPoorHint2(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mSeqToArrayPoorHint2(String item) {
        return "mSeqToArrayPoorHint2(String " + item + ")";
    }
    
    public String mSeqToArrayPoorHint3(String[] items) {
        return "mSeqToArrayPoorHint3(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mSeqToArrayPoorHint3(int[] items) {
        return "mSeqToArrayPoorHint3(int[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVsListPreference(String[] items) {
        return "mStringArrayVsListPreference(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVsListPreference(List items) {
        return "mStringArrayVsListPreference(List " + Helpers.listToString(items) + ")";
    }

    public String mStringArrayVsObjectArrayPreference(String[] items) {
        return "mStringArrayVsObjectArrayPreference(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVsObjectArrayPreference(Object[] items) {
        return "mStringArrayVsObjectArrayPreference(Object[] " + Helpers.arrayToString(items) + ")";
    }

    public String mIntArrayVsIntegerArrayPreference(int[] items) {
        return "mIntArrayVsIntegerArrayPreference(int[] " + Helpers.arrayToString(items) + ")";
    }

    public String mIntArrayVsIntegerArrayPreference(Integer[] items) {
        return "mIntArrayVsIntegerArrayPreference(Integer[] " + Helpers.arrayToString(items) + ")";
    }
    
    public String mIntArrayNonOverloaded(int[] items) {
        return "mIntArrayNonOverloaded(int[] " + Helpers.arrayToString(items) + ")";
    }

    public String mIntegerArrayNonOverloaded(Integer[] items) {
        return "mIntegerArrayNonOverloaded(Integer[] " + Helpers.arrayToString(items) + ")";
    }

    public String mIntegerListNonOverloaded(List<Integer> items) {
        return "mIntegerListNonOverloaded(List<Integer> " + items + ")";
    }

    public String mStringListNonOverloaded(List<String> items) {
        return "mStringListNonOverloaded(List<String> " + items + ")";
    }

    public String mStringArrayNonOverloaded(String[] items) {
        return "mStringArrayNonOverloaded(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mObjectListNonOverloaded(List<Object> items) {
        return "mObjectListNonOverloaded(List<Object> " + items + ")";
    }

    public String mObjectArrayNonOverloaded(Object[] items) {
        return "mObjectArrayNonOverloaded(Object[] " + Helpers.arrayToString(items) + ")";
    }

    public String mIntegerArrayOverloaded(Integer[] items, int i) {
        return "mIntegerArrayOverloaded(Integer[] " + Helpers.arrayToString(items) + ", int " + i + ")";
    }

    public String mIntegerArrayOverloaded(Object obj, boolean b) {
        return "mIntegerArrayOverloaded(Object " + obj + ", boolean " + b + ")";
    }

    public String mStringArrayOverloaded(String[] items, int i) {
        return "mStringArrayOverloaded(String[] " + Helpers.arrayToString(items) + ", int " + i + ")";
    }

    public String mStringArrayOverloaded(Object obj, boolean b) {
        return "mStringArrayOverloaded(Object " + obj + ", boolean " + b + ")";
    }

    public String mCharArrayOverloaded(char[] items, int i) {
        return "mCharArrayOverloaded(char[] " + Helpers.arrayToString(items) + ", int " + i + ")";
    }

    public String mCharArrayOverloaded(Character[] items, String s) {
        return "mCharArrayOverloaded(Character[] " + Helpers.arrayToString(items) + ", String " + s + ")";
    }
    
    public String mCharArrayOverloaded(Object obj, boolean b) {
        return "mCharArrayOverloaded(Object " + obj + ", boolean " + b + ")";
    }

    public String mStringArrayArrayOverloaded(String[][] arrayArray, int i) {
        return "mStringArrayArrayOverloaded(String[][] " + Helpers.arrayToString(arrayArray) + ", int " + i + ")";
    }
    
    public String mStringArrayArrayOverloaded(Object obj, boolean b) {
        return "mStringArrayArrayOverloaded(Object " + obj + ", boolean " + b + ")";
    }
    
    public String mIntArrayArrayOverloaded(int[][] xss) {
        return "mIntArrayArrayOverloaded(" + Helpers.arrayToString(xss) + ")";
    }

    public String mIntArrayArrayOverloaded(String s) {
        return "mIntArrayArrayOverloaded(" + s + ")";
    }
    
    public String mArrayOfListsOverloaded(List[] xss) {
        return "mArrayOfListsOverloaded(" + Helpers.arrayToString(xss) + ")";
    }

    public String mArrayOfListsOverloaded(String x) {
        return "mArrayOfListsOverloaded(" + x + ")";
    }
    
    public String mIntArrayArrayNonOverloaded(int[][] xss) {
        return "mIntArrayArrayNonOverloaded(" + Helpers.arrayToString(xss) + ")";
    }

    public String mArrayOfListsNonOverloaded(List[] xss) {
        return "mArrayOfListsNonOverloaded(" + Helpers.arrayToString(xss) + ")";
    }
    
    public String mStringArrayVarargsNonOverloaded(String... items) {
        return "mStringArrayVarargsNonOverloaded(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVarargsOverloaded(String... items) {
        return "mStringArrayVarargsNonOverloaded(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVarargsOverloaded1(String... items) {
        return "mStringArrayVarargsOverloaded1(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVarargsOverloaded1(List<String> items) {
        return "mStringArrayVarargsOverloaded1(List " + Helpers.listToString(items) + ")";
    }

    public String mStringArrayVarargsOverloaded2(String... items) {
        return "mStringArrayVarargsOverloaded2(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVarargsOverloaded2(String item) {
        return "mStringArrayVarargsOverloaded2(String " + item + ")";
    }
    
    public String mStringArrayVarargsOverloaded3(String... items) {
        return "mStringArrayVarargsOverloaded3(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVarargsOverloaded3(String item1, String item2) {
        return "mStringArrayVarargsOverloaded3(String " + item1 + ", String " + item2 + ")";
    }
    
    public String mStringArrayVarargsOverloaded4(String... items) {
        return "mStringArrayVarargsOverloaded4(String[] " + Helpers.arrayToString(items) + ")";
    }

    public String mStringArrayVarargsOverloaded4(List... items) {
        return "mStringArrayVarargsOverloaded4(List[] " + Helpers.arrayToString(items) + ")";
    }
    
    public String mListOrString(List<String> items) {
        return "mListOrString(List " + Helpers.listToString(items) + ")";
    }

    public String mListOrString(String item) {
        return "mListOrString(String " + item + ")";
    }

    public String mListListOrString(List<List<Object>> items) {
        return "mListListOrString(List " + Helpers.listToString(items) + ")";
    }

    public String mListListOrString(String item) {
        return "mListListOrString(String " + item + ")";
    }
    
    public String mMapOrBoolean(Map v) {
        return "mMapOrBoolean(Map " + v +")";
    }

    public String mMapOrBoolean(boolean v) {
        return "mMapOrBoolean(boolean " + v + ")";
    }

    public String mMapOrBooleanVarargs(Map... v) {
        return "mMapOrBooleanVarargs(Map... " + Helpers.arrayToString(v) +")";
    }

    public String mMapOrBooleanVarargs(boolean... v) {
        return "mMapOrBooleanVarargs(boolean... " + Helpers.arrayToString(v) + ")";
    }

    public String mMapOrBooleanFixedAndVarargs(Map v) {
        return "mMapOrBooleanFixedAndVarargs(Map " + v +")";
    }

    public String mMapOrBooleanFixedAndVarargs(boolean v) {
        return "mMapOrBooleanFixedAndVarargs(boolean " + v + ")";
    }

    public String mMapOrBooleanFixedAndVarargs(Map... v) {
        return "mMapOrBooleanFixedAndVarargs(Map... " + Helpers.arrayToString(v) +")";
    }

    public String mMapOrBooleanFixedAndVarargs(boolean... v) {
        return "mMapOrBooleanFixedAndVarargs(boolean... " + Helpers.arrayToString(v) + ")";
    }
    
    public String mNumberOrArray(Number v) {
        return "mNumberOrArray(Number " + v + ")";
    }

    public String mNumberOrArray(Object[] v) {
        return "mNumberOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }
    
    public String mIntOrArray(int v) {
        return "mIntOrArray(int " + v + ")";
    }

    public String mIntOrArray(Object[] v) {
        return "mIntOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }

    public String mDateOrArray(Date v) {
        return "mDateOrArray(Date " + v.getTime() + ")";
    }

    public String mDateOrArray(Object[] v) {
        return "mDateOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }
    
    public String mStringOrArray(String v) {
        return "mStringOrArray(String " + v + ")";
    }

    public String mStringOrArray(Object[] v) {
        return "mStringOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }
    
    public String mBooleanOrArray(boolean v) {
        return "mBooleanOrArray(boolean " + v + ")";
    }

    public String mBooleanOrArray(Object[] v) {
        return "mBooleanOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }
    
    public String mMapOrArray(Map v) {
        return "mMapOrArray(Map " + v + ")";
    }

    public String mMapOrArray(Object[] v) {
        return "mMapOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }
    
    public String mListOrArray(List v) {
        return "mListOrArray(List " + v + ")";
    }

    public String mListOrArray(Object[] v) {
        return "mListOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }
    
    public String mSetOrArray(Set v) {
        return "mSetOrArray(Set " + v + ")";
    }

    public String mSetOrArray(Object[] v) {
        return "mSetOrArray(Object[] " + Helpers.arrayToString(v) + ")";
    }
    
    public String mCharNonOverloaded(char c) {
        return "mCharNonOverloaded(char " + c + ")";
    }

    public String mCharacterNonOverloaded(Character c) {
        return "mCharacterNonOverloaded(Character " + c + ")";
    }
    
    public String mCharOrCharacterOverloaded(char c) {
        return "mCharOrCharacterOverloaded(char " + c + ")";
    }

    public String mCharOrCharacterOverloaded(Character c) {
        return "mCharOrCharacterOverloaded(Character " + c + ")";
    }

    public String mCharOrBooleanOverloaded(char c) {
        return "mCharOrBooleanOverloaded(char " + c + ")";
    }

    public String mCharOrBooleanOverloaded(boolean b) {
        return "mCharOrBooleanOverloaded(boolean " + b + ")";
    }

    public String mCharOrStringOverloaded(char c, boolean b) {
        return "mCharOrStringOverloaded(char " + c + ", boolean " + b + ")";
    }

    public String mCharOrStringOverloaded(String s, int i) {
        return "mCharOrStringOverloaded(String " + s + ", int " + i + ")";
    }

    public String mCharacterOrStringOverloaded(Character c, boolean b) {
        return "mCharacterOrStringOverloaded(Character " + c + ", boolean " + b + ")";
    }

    public String mCharacterOrStringOverloaded(String s, int i) {
        return "mCharacterOrStringOverloaded(String " + s + ", int " + i + ")";
    }
    
    public String mCharOrStringOverloaded2(String s) {
        return "mCharOrStringOverloaded2(String " + s + ")";
    }

    public String mCharOrStringOverloaded2(char c) {
        return "mCharOrStringOverloaded2(char " + c + ")";
    }
    
    public String mCharacterOrStringOverloaded2(String s) {
        return "mCharacterOrStringOverloaded2(String " + s + ")";
    }

    public String mCharacterOrStringOverloaded2(Character c) {
        return "mCharacterOrStringOverloaded2(Character " + c + ")";
    }
    

    public String getJavaString() {
        return "s";
    }
    
    public List getJavaStringList() {
        List list = new ArrayList();
        list.add("a");
        list.add("b");
        return list;
    }

    public List getJavaString2List() {
        List list = new ArrayList();
        list.add("aa");
        list.add("bb");
        return list;
    }

    public List getJavaStringListList() {
        List listList = new ArrayList();
        {
            List list = new ArrayList();
            list.add("a");
            list.add("b");
            
            listList.add(list);
        }
        {
            List list = new ArrayList();
            list.add("c");
            
            listList.add(list);
        }
        return listList;
    }

    public List getJavaStringSequenceList() throws TemplateModelException {
        ObjectWrapper ow = Environment.getCurrentEnvironment().getObjectWrapper();
        
        List listList = new ArrayList();
        {
            List list = new ArrayList();
            list.add("a");
            list.add("b");
            
            listList.add(ow.wrap(list));
        }
        {
            List list = new ArrayList();
            list.add("c");
            
            listList.add(ow.wrap(list));
        }
        return listList;
    }
    
    public List<int[]> getJavaListOfIntArrays() {
        List list = new ArrayList();
        list.add(new int[] {1, 2, 3});
        list.add(new int[] {});
        list.add(new int[] {4});
        return list;
    }
    
    @SuppressWarnings("boxing")
    public List getJavaIntegerListList() {
        List listList = new ArrayList();
        {
            List list = new ArrayList();
            list.add(1);
            list.add(2);
            
            listList.add(list);
        }
        {
            List list = new ArrayList();
            list.add(3);
            
            listList.add(list);
        }
        return listList;
    }
    
    @SuppressWarnings("boxing")
    public List<Integer> getJavaIntegerList() {
        List<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        return list;
    }

    @SuppressWarnings("boxing")
    public List<Byte> getJavaByteList() {
        List<Byte> list = new ArrayList<Byte>();
        list.add((byte) 1);
        list.add((byte) 2);
        return list;
    }

    @SuppressWarnings("boxing")
    public List<Character> getJavaCharacterList() {
        List<Character> list = new ArrayList<Character>();
        list.add('c');
        list.add('C');
        return list;
    }
    
    public String[] getJavaStringArray() {
        return new String[] { "a", "b" };
    }

    public int[] getJavaIntArray() {
        return new int[] { 11, 22 };
    }

    public Integer[] getJavaIntegerArray() {
        return new Integer[] { Integer.valueOf(11), Integer.valueOf(22) };
    }
    
    public String[] getJavaEmptyStringArray() {
        return new String[] { };
    }
    
    public String[][] getJavaStringArrayArray() {
        return new String[][] { new String[] { "a", "b" }, new String[] { }, new String[] { "c" } };
    }
    
    public Object[] getJavaObjectArray() {
        return new Object[] { "a", "b" };
    }
    
    public TemplateModel getHashAndScalarModel() {
        return HashAndScalarModel.INSTANCE;
    }

    public TemplateModel getBooleanAndScalarModel() {
        return BooleanAndScalarModel.INSTANCE;
    }
    
    public TemplateModel getAllModels() {
        return AllTemplateModels.INSTANCE;
    }

    public TemplateNumberModel getAdaptedNumber() {
        return new MyAdapterNumberModel();
    }

    public TemplateNumberModel getWrapperNumber() {
        return new MyWrapperNumberModel();
    }

    public TemplateBooleanModel getStringAdaptedToBoolean() {
        return new MyStringAdaptedToBooleanModel();
    }
    
    public TemplateBooleanModel getStringAdaptedToBoolean2() {
        return new MyStringAdaptedToBooleanModel2();
    }
    
    public TemplateBooleanModel getStringWrappedAsBoolean() {
        return new MyStringWrapperAsBooleanModel();
    }
    
    public TemplateBooleanModel getBooleanWrappedAsAnotherBoolean() {
        return new MyBooleanWrapperAsAnotherBooleanModel(); 
    }
    
    public String bugReport363(Map<String, ? extends Object> fields, List<?> listField) {
        return "Executed: testMethod(Map fields, List listField) on input: fields=" + fields
                + " and listField=" + listField;
    }

    public String bugReport363(Object... fields) {
        return "Executed: testMethod(Object... fields) on input: fields=" + Helpers.arrayToString(fields);
    }
    
    private static class MyAdapterNumberModel implements TemplateNumberModel, AdapterTemplateModel {

        public Object getAdaptedObject(Class hint) {
            if (hint == double.class) {
                return Double.valueOf(123.0001);
            } else if (hint == Double.class) {
                return Double.valueOf(123.0002);
            } else {
                return Long.valueOf(124L);
            }
        }

        public Number getAsNumber() throws TemplateModelException {
            return Integer.valueOf(122);
        }
        
    }
    
    private static class MyWrapperNumberModel implements TemplateNumberModel, WrapperTemplateModel {

        public Number getAsNumber() throws TemplateModelException {
            return Integer.valueOf(122);
        }

        public Object getWrappedObject() {
            return Double.valueOf(123.0001);
        }
        
    }
    
    private static class MyStringWrapperAsBooleanModel implements TemplateBooleanModel, WrapperTemplateModel {

        public Object getWrappedObject() {
            return "yes";
        }

        public boolean getAsBoolean() throws TemplateModelException {
            return true;
        }
        
    }

    private static class MyBooleanWrapperAsAnotherBooleanModel implements TemplateBooleanModel, WrapperTemplateModel {

        public Object getWrappedObject() {
            return Boolean.TRUE;
        }

        public boolean getAsBoolean() throws TemplateModelException {
            return false;
        }
        
    }
    
    private static class MyStringAdaptedToBooleanModel implements TemplateBooleanModel, AdapterTemplateModel {

        public Object getAdaptedObject(Class hint) {
            if (hint != Boolean.class && hint != boolean.class) {
                return "yes";
            } else {
                return Boolean.TRUE;
            }
        }

        public boolean getAsBoolean() throws TemplateModelException {
            return false;
        }
        
    }

    private static class MyStringAdaptedToBooleanModel2 implements TemplateBooleanModel, AdapterTemplateModel {

        public Object getAdaptedObject(Class hint) {
            return "yes";
        }

        public boolean getAsBoolean() throws TemplateModelException {
            return true;
        }
        
    }
    
}
