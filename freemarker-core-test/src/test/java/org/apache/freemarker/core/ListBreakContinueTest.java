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
        testHash(ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)); // Listing a TemplateHashModelEx2
        testHash(new NonEx2Hash((TemplateHashModelEx) getConfiguration().getObjectWrapper().wrap(
                ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)))); // Listing a TemplateHashModelEx (non-Ex2)
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
    
    /** Hides the Ex2 features of another hash */
    // TODO [FM3][CF] Remove
    static class NonEx2Hash implements TemplateHashModelEx {
        private final TemplateHashModelEx delegate;

        public NonEx2Hash(TemplateHashModelEx delegate) {
            this.delegate = delegate;
        }

        @Override
        public TemplateModel get(String key) throws TemplateException {
            return delegate.get(key);
        }

        @Override
        public int getHashSize() throws TemplateException {
            return delegate.getHashSize();
        }

        @Override
        public TemplateCollectionModel keys() throws TemplateException {
            return delegate.keys();
        }

        @Override
        public boolean isEmptyHash() throws TemplateException {
            return delegate.isEmptyHash();
        }

        @Override
        public TemplateCollectionModel values() throws TemplateException {
            return delegate.values();
        }

        @Override
        public KeyValuePairIterator keyValuePairIterator() throws TemplateException {
            return delegate.keyValuePairIterator();
        }
    }
    
}
