package freemarker.test.templatesuite.models;

import java.io.File;

public class OverloadedMethods2 {

    public String mVarargs(String... a1) {
        StringBuilder sb = new StringBuilder();
        for (String s : a1) {
            sb.append(s);
        }
        return "mVarargs(String... a1 = " + sb + ")";
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

    public String mIntPrimVSBoxed(int a1) {
        return "mIntPrimVSBoxed(int a1 = " + a1 + ")";
    }
    
    public String mIntPrimVSBoxed(Integer a1) {
        return "mIntPrimVSBoxed(Integer a1 = " + a1 + ")";
    }

    public int mIntNonOverloaded(int a1) {
        return a1;
    }

    public String mNumPrimVSPrim(short a1) {
        return "mNum(short a1 = " + a1 + ")";
    }
    
    public String mNumPrimVSPrim(long a1) {
        return "mNum(long a1 = " + a1 + ")";
    }

    public String mNumBoxedVSBoxed(Short a1) {
        return "mNum(Short a1 = " + a1 + ")";
    }
    
    public String mNumBoxedVSBoxed(Long a1) {
        return "mNum(Long a1 = " + a1 + ")";
    }

    public String mNumUnambigous(Short a1, boolean otherOverload) {
        return "mmNumUnambigous won't be called";
    }
    
    public String mNumUnambigous(Integer a1) {
        return "mNumUnambigous(Integer a1 = " + a1 + ")";
    }
    
}
