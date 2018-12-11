/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class DirectiveCallPlaceTest extends TemplateTest {

    @Test
    public void testCustomDataBasics() throws IOException, TemplateException {
        addTemplate(
                "customDataBasics.f3ah",
                "<@uc>Abc</@uc> <@uc>x=${x}</@uc> <@uc>Ab<#-- -->c</@uc> <@lc/><@lc></@lc> <@lc>Abc</@lc>");
        
        CachingTextConverterDirective.resetCacheRecreationCount();
        for (int i = 0; i < 3; i++) {
            assertOutputForNamed(
                    "customDataBasics.f3ah",
                    "ABC[cached 1] X=123 ABC[cached 2]  abc[cached 3]");
        }
    }

    @Test
    public void testCustomDataProviderMismatch() throws IOException, TemplateException {
        addTemplate(
                "customDataProviderMismatch.f3ah",
                "<#list [uc, lc, uc] as d><#list 1..2 as _><@d>Abc</@d></#list></#list>");
        
        CachingTextConverterDirective.resetCacheRecreationCount();
        assertOutputForNamed(
                "customDataProviderMismatch.f3ah",
                "ABC[cached 1]ABC[cached 1]abc[cached 2]abc[cached 2]ABC[cached 3]ABC[cached 3]");
        assertOutputForNamed(
                "customDataProviderMismatch.f3ah",
                "ABC[cached 3]ABC[cached 3]abc[cached 4]abc[cached 4]ABC[cached 5]ABC[cached 5]");
    }
    
    @Test
    public void testPositions() throws IOException, TemplateException {
        addTemplate(
                "positions.f3ah",
                "<@pa />\n"
                + "..<@pa\n"
                + "/><@pa>xxx</@>\n"
                + "<@pa>{<@pa/> <@pa/>}</@>\n");
        
        assertOutputForNamed(
                "positions.f3ah",
                "[positions.f3ah:1:1-1:7]"
                + "..[positions.f3ah:2:3-3:2]"
                + "[positions.f3ah:3:3-3:14]xxx\n"
                + "[positions.f3ah:4:1-4:24]{[positions.f3ah:4:7-4:12] [positions.f3ah:4:14-4:19]}\n");
    }
    
    @SuppressWarnings("boxing")
    @Override
    protected Object createDataModel() {
        Map<String, Object> dm = new HashMap<>();
        dm.put("uc", new CachingUpperCaseDirective());
        dm.put("lc", new CachingLowerCaseDirective());
        dm.put("pa", new PositionAwareDirective());
        dm.put("argP", new ArgPrinterDirective());
        dm.put("x", 123);
        return dm;
    }

    private abstract static class CachingTextConverterDirective implements TemplateDirectiveModel {

        /** Only needed for testing. */
        private static AtomicInteger cacheRecreationCount = new AtomicInteger();
        
        /** Only needed for testing. */
        static void resetCacheRecreationCount() {
            cacheRecreationCount.set(0);
        }

        @Override
        public void execute(TemplateModel[] args, final CallPlace callPlace, Writer out, final Environment env)
                throws TemplateException, IOException {
            if (!callPlace.hasNestedContent()) {
                return;
            }
            
            final String convertedText;
            if (callPlace.isNestedOutputCacheable()) {
                try {
                    convertedText = (String) callPlace.getOrCreateCustomData(
                            getTextConversionIdentity(), new CommonSupplier<String>() {

                                @Override
                                public String get() throws TemplateException, IOException {
                                    return convertBodyText(callPlace, env)
                                            + "[cached " + cacheRecreationCount.incrementAndGet() + "]";
                                }

                            });
                } catch (CallPlaceCustomDataInitializationException e) {
                    throw new TemplateException("Failed to pre-render nested content", e);
                }
            } else {
                convertedText = convertBodyText(callPlace, env);
            }

            out.write(convertedText);
        }

        @Override
        public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
            return ArgumentArrayLayout.PARAMETERLESS;
        }

        @Override
        public boolean isNestedContentSupported() {
            return true;
        }

        protected abstract Class getTextConversionIdentity();

        private String convertBodyText(CallPlace callPlace, Environment env) throws TemplateException,
                IOException {
            StringWriter sw = new StringWriter();
            callPlace.executeNestedContent(null, sw, env);
            return convertText(sw.toString());
        }
        
        protected abstract String convertText(String s);

    }
    
    private static class CachingUpperCaseDirective extends CachingTextConverterDirective {

        @Override
        protected String convertText(String s) {
            return s.toUpperCase();
        }
        
        @Override
        protected Class getTextConversionIdentity() {
            return CachingUpperCaseDirective.class;
        }
        
    }

    private static class CachingLowerCaseDirective extends CachingTextConverterDirective {

        @Override
        protected String convertText(String s) {
            return s.toLowerCase();
        }

        @Override
        protected Class getTextConversionIdentity() {
            return CachingLowerCaseDirective.class;
        }
        
    }
    
    private static class PositionAwareDirective implements TemplateDirectiveModel {

        @Override
        public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                throws TemplateException, IOException {
            out.write("[");
            out.write(getTemplateSourceName(callPlace));
            out.write(":");
            out.write(Integer.toString(callPlace.getBeginLine()));
            out.write(":");
            out.write(Integer.toString(callPlace.getBeginColumn()));
            out.write("-");
            out.write(Integer.toString(callPlace.getEndLine()));
            out.write(":");
            out.write(Integer.toString(callPlace.getEndColumn()));
            out.write("]");
            callPlace.executeNestedContent(null, out, env);
        }

        @Override
        public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
            return ArgumentArrayLayout.PARAMETERLESS;
        }

        @Override
        public boolean isNestedContentSupported() {
            return true;
        }

        private String getTemplateSourceName(CallPlace callPlace) {
            return callPlace.getTemplate().getSourceName();
        }
    }

    private static class ArgPrinterDirective implements TemplateDirectiveModel {

        private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
                0, false,
                null, true
        );

        @Override
        public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
                throws TemplateException, IOException {
            TemplateHashModelEx varargs = (TemplateHashModelEx) args[ARGS_LAYOUT.getNamedVarargsArgumentIndex()];
            if (varargs.getHashSize() > 0) {
                out.write("(p=");
                out.write(((TemplateStringModel) varargs.get("p")).getAsString());
                out.write(")");
            }
            if (callPlace.hasNestedContent()) {
                out.write("{");
                callPlace.executeNestedContent(null, out, env);
                out.write("}");
            }
        }

        @Override
        public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
            return ARGS_LAYOUT;
        }

        @Override
        public boolean isNestedContentSupported() {
            return true;
        }

    }

}
