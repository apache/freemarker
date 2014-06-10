package freemarker.core.subpkg;

public class PublicWithMixedConstructors {
    
    private final String s;

    public PublicWithMixedConstructors(Integer x) {
        s = "Integer";
    }

    PublicWithMixedConstructors(int x) {
        s = "int";
    }

    public String getS() {
        return s;
    }
    
}
