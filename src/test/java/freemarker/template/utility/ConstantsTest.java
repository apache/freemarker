package freemarker.template.utility;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public final class ConstantsTest extends TemplateTest {
    
    @Test
    public void testEmptyHash() throws IOException, TemplateException {
        addToDataModel("h", Constants.EMPTY_HASH);
        assertOutput("{<#list h as k ,v>x</#list>}", "{}"); 
        assertOutput("{<#list h?keys as k>x</#list>}", "{}"); 
        assertOutput("{<#list h?values as k>x</#list>}", "{}"); 
        assertOutput("${h?size}", "0"); 
    }
    
}
