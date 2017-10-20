package org.apache.freemarker.core;

import java.io.IOException;

import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ListBreakContinueTest extends TemplateTest {
    
    @Test
    public void testNonHash() throws IOException, TemplateException {
        addToDataModel("listed", ImmutableSet.of(1, 2, 3, 4, 5));
        assertOutput(
                "<#list listed as i>B(${i}) <#if i == 3>Break!<#break></#if>A(${i})<#sep>, </#list>",
                "B(1) A(1), B(2) A(2), B(3) Break!");
        assertOutput(
                "<#list listed as i>B(${i}) <#if i == 3>Continue! <#continue></#if>A(${i})<#sep>, </#list>",
                "B(1) A(1), B(2) A(2), B(3) Continue! B(4) A(4), B(5) A(5)");
    }

    @Test
    public void testHash() throws IOException, TemplateException {
        testHash(ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)); // Listing a TemplateHashModelEx
    }

    private void testHash(Object listed) throws IOException, TemplateException {
        addToDataModel("listed", listed);
        assertOutput(
                "<#list listed as k, v>B(${k}=${v}) <#if k == 'c'>Break!<#break></#if>A(${k}=${v})<#sep>, </#list>",
                "B(a=1) A(a=1), B(b=2) A(b=2), B(c=3) Break!");
        assertOutput(
                "<#list listed as k, v>B(${k}=${v}) <#if k == 'c'>Continue! <#continue></#if>A(${k}=${v})<#sep>, </#list>",
                "B(a=1) A(a=1), B(b=2) A(b=2), B(c=3) Continue! B(d=4) A(d=4), B(e=5) A(e=5)");
    }
    
}
