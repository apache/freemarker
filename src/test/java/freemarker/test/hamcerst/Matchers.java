package freemarker.test.hamcerst;

import org.hamcrest.Matcher;

public final class Matchers {
    
    private Matchers() {
        // Not meant to be instantiated
    }

    public static Matcher<String> containsStringIgnoringCase(String substring)
    {
        return StringContainsIgnoringCase.containsStringIgnoringCase(substring);
    }

}
