package freemarker.test.hamcerst;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.SubstringMatcher;

public class StringContainsIgnoringCase extends SubstringMatcher {
    
    public StringContainsIgnoringCase(String substring) {
        super(substring);
    }

    protected boolean evalSubstringOf(String s) {
        return s.toLowerCase().contains(this.substring.toLowerCase());
    }

    protected String relationship() {
        return "containing ignoring case";
    }

    @Factory
    public static Matcher<String> containsStringIgnoringCase(String substring) {
        return new StringContainsIgnoringCase(substring);
    }
    
}