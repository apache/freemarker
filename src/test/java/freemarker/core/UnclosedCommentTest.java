package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class UnclosedCommentTest extends TemplateTest {
    
    private static final String UNCLOSED_COMMENT_0 = "foo<#--";
    private static final String UNCLOSED_COMMENT_1 = "foo<#-- ";
    private static final String UNCLOSED_COMMENT_2 = "foo<#--bar";
    private static final String UNCLOSED_COMMENT_3 = "foo\n<#--\n";

    private static final String UNCLOSED_NOPARSE_0 = "foo<#noparse>";
    private static final String UNCLOSED_NOPARSE_1 = "foo<#noparse> ";
    private static final String UNCLOSED_NOPARSE_2 = "foo<#noparse>bar";
    private static final String UNCLOSED_NOPARSE_3 = "foo\n<#noparse>\n";
    
    @Test
    public void testLegacyBehavior() throws IOException, TemplateException {
        setConfiguration(new Configuration(Configuration.VERSION_2_3_20));
        assertErrorContains(UNCLOSED_COMMENT_0, "end of file");
        assertOutput(UNCLOSED_COMMENT_1, "foo");
        assertOutput(UNCLOSED_COMMENT_2, "foo");
        assertOutput(UNCLOSED_COMMENT_3, "foo\n");
        assertErrorContains(UNCLOSED_NOPARSE_0, "end of file");
        assertOutput(UNCLOSED_NOPARSE_1, "foo");
        assertOutput(UNCLOSED_NOPARSE_2, "foo");
        assertOutput(UNCLOSED_NOPARSE_3, "foo\n");
    }

    @Test
    public void testFixedBehavior() throws IOException, TemplateException {
        setConfiguration(new Configuration(Configuration.VERSION_2_3_21));
        assertErrorContains(UNCLOSED_COMMENT_0, "end of file");  // Not too good...
        assertErrorContains(UNCLOSED_COMMENT_1, "Unclosed", "<#--");
        assertErrorContains(UNCLOSED_COMMENT_2, "Unclosed", "<#--");
        assertErrorContains(UNCLOSED_COMMENT_3, "Unclosed", "<#--");
        assertErrorContains(UNCLOSED_NOPARSE_0, "end of file");  // Not too good...
        assertErrorContains(UNCLOSED_NOPARSE_1, "Unclosed", "#noparse");
        assertErrorContains(UNCLOSED_NOPARSE_2, "Unclosed", "#noparse");
        assertErrorContains(UNCLOSED_NOPARSE_3, "Unclosed", "#noparse");
    }
    
}
