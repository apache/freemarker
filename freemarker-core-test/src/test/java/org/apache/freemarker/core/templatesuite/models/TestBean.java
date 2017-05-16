package org.apache.freemarker.core.templatesuite.models;

public class TestBean {

    public int m(int n) {
        return n * 10;
    }

    public int mOverloaded(int n) {
        return n * 10;
    }

    public String mOverloaded(String s) {
        return s.toUpperCase();
    }

}
