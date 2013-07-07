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
    
}
