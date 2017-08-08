package org.apache.freemarker.core.model.impl.org.apache.freemarker.core.model;

import java.io.IOException;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public final class ConstantsTest extends TemplateTest {

    @Test
    public void testEmptyHash() throws IOException, TemplateException {
        addToDataModel("h", TemplateHashModel.EMPTY_HASH);
        assertOutput("{<#list h as k ,v>x</#list>}", "{}");
        assertOutput("{<#list h?keys as k>x</#list>}", "{}");
        assertOutput("{<#list h?values as k>x</#list>}", "{}");
        assertOutput("${h?size}", "0");
    }

}
