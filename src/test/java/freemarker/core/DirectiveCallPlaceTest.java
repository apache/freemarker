/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.ObjectFactory;
import freemarker.test.TemplateTest;

public class DirectiveCallPlaceTest extends TemplateTest {
    
    private static final Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
    static {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate(
                "customDataBasics.ftl",
                "<@uc>Abc</@uc> <@uc>x=${x}</@uc> <@uc>Ab<#-- -->c</@uc> <@lc/><@lc></@lc> <@lc>Abc</@lc>");
        tl.putTemplate(
                "customDataProviderMismatch.ftl",
                "<#list [uc, lc, uc] as d><#list 1..2 as _><@d>Abc</@d></#list></#list>");
        tl.putTemplate(
                "positions.ftl",
                "<@pa />\n"
                + "..<@pa\n"
                + "/><@pa>xxx</@>\n"
                + "<@pa>{<@pa/> <@pa/>}</@>\n"
                + "${curDirLine}<@argP p=curDirLine?string>${curDirLine}</@argP>${curDirLine}\n"
                + "<#macro m p>(p=${p}){<#nested>}</#macro>\n"
                + "${curDirLine}<@m p=curDirLine?string>${curDirLine}</@m>${curDirLine}");
        cfg.setTemplateLoader(tl);
    }
    
    @Test
    public void testCustomDataBasics() throws IOException, TemplateException {
        setConfiguration(cfg);
        CachingTextConverterDirective.resetCacheRecreationCount();
        for (int i = 0; i < 3; i++) {
            assertOutputForNamed(
                    "customDataBasics.ftl",
                    "ABC[cached 1] X=123 ABC[cached 2]  abc[cached 3]");
        }
    }

    @Test
    public void testCustomDataProviderMismatch() throws IOException, TemplateException {
        setConfiguration(cfg);
        CachingTextConverterDirective.resetCacheRecreationCount();
        assertOutputForNamed(
                "customDataProviderMismatch.ftl",
                "ABC[cached 1]ABC[cached 1]abc[cached 2]abc[cached 2]ABC[cached 3]ABC[cached 3]");
        assertOutputForNamed(
                "customDataProviderMismatch.ftl",
                "ABC[cached 3]ABC[cached 3]abc[cached 4]abc[cached 4]ABC[cached 5]ABC[cached 5]");
    }
    
    @Test
    public void testPositions() throws IOException, TemplateException {
        setConfiguration(cfg);
        assertOutputForNamed(
                "positions.ftl",
                "[positions.ftl:1:1-1:7]"
                + "..[positions.ftl:2:3-3:2]"
                + "[positions.ftl:3:3-3:14]xxx\n"
                + "[positions.ftl:4:1-4:24]{[positions.ftl:4:7-4:12] [positions.ftl:4:14-4:19]}\n"
                + "-(p=5){-}-\n"
                + "-(p=7){-}-"
                );
    }
    
    @SuppressWarnings("boxing")
    @Override
    protected Object createDataModel() {
        Map<String, Object> dm = new HashMap<String, Object>();
        dm.put("uc", new CachingUpperCaseDirective());
        dm.put("lc", new CachingLowerCaseDirective());
        dm.put("pa", new PositionAwareDirective());
        dm.put("argP", new ArgPrinterDirective());
        dm.put("curDirLine", new CurDirLineScalar());
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
        
        public void execute(Environment env, Map params, TemplateModel[] loopVars, final TemplateDirectiveBody body)
                throws TemplateException, IOException {
            if (body == null) {
                return;
            }
            
            final String convertedText;

            final DirectiveCallPlace callPlace = env.getCurrentDirectiveCallPlace();
            if (callPlace.isNestedOutputCacheable()) {
                try {
                    convertedText = (String) callPlace.getOrCreateCustomData(
                            getTextConversionIdentity(), new ObjectFactory/* <String> */() {

                                public Object createObject() throws TemplateException, IOException {
                                    return convertBodyText(body)
                                            + "[cached " + cacheRecreationCount.incrementAndGet() + "]";
                                }

                            });
                } catch (CallPlaceCustomDataInitializationException e) {
                    throw new TemplateModelException("Failed to pre-render nested content", e);
                }
            } else {
                convertedText = convertBodyText(body);
            }

            env.getOut().write(convertedText);
        }

        protected abstract Class getTextConversionIdentity();

        private String convertBodyText(TemplateDirectiveBody body) throws TemplateException,
                IOException {
            StringWriter sw = new StringWriter();
            body.render(sw);
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

        public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                throws TemplateException, IOException {
            Writer out = env.getOut();
            DirectiveCallPlace callPlace = env.getCurrentDirectiveCallPlace();
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
            if (body != null) {
                body.render(out);
            }
        }

        private String getTemplateSourceName(DirectiveCallPlace callPlace) {
            return ((UnifiedCall) callPlace).getTemplate().getSourceName();
        }
        
    }

    private static class ArgPrinterDirective implements TemplateDirectiveModel {

        public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                throws TemplateException, IOException {
            final Writer out = env.getOut();
            if (params.size() > 0) {
                out.write("(p=");
                out.write(((TemplateScalarModel) params.get("p")).getAsString());
                out.write(")");
            }
            if (body != null) {
                out.write("{");
                body.render(out);
                out.write("}");
            }
        }
        
    }
    
    private static class CurDirLineScalar implements TemplateScalarModel {

        public String getAsString() throws TemplateModelException {
            DirectiveCallPlace callPlace = Environment.getCurrentEnvironment().getCurrentDirectiveCallPlace();
            return callPlace != null
                    ? String.valueOf(Environment.getCurrentEnvironment().getCurrentDirectiveCallPlace().getBeginLine())
                    : "-";
        }
        
    }

}
