package freemarker.core;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ObjectFactory;
import freemarker.test.TemplateTest;

public class DirectiveCallPlaceTest extends TemplateTest {
    
    private final Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
    {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate(
                "customDataBasics",
                "<@uc>Abc</@uc> <@uc>x=${x}</@uc> <@uc>Ab<#-- -->c</@uc>");
        tl.putTemplate(
                "customDataProviderMismatch",
                "<#list [uc, lc, uc] as d><#list 1..2 as _><@d>Abc</@d></#list></#list>");
        cfg.setTemplateLoader(tl);
    }
    
    @Test
    public void testCustomDataBasics() throws IOException, TemplateException {
        setConfiguration(cfg);
        CachingTextConverterDirective.resetCacheRecreationCount();
        for (int i = 0; i < 3; i++) {
            assertOutputForNamed(
                    "customDataBasics",
                    "ABC[cached 1] X=123 ABC[cached 2]");
        }
    }

    @Test
    public void testCustomDataProviderMismatch() throws IOException, TemplateException {
        setConfiguration(cfg);
        CachingTextConverterDirective.resetCacheRecreationCount();
        assertOutputForNamed(
                "customDataProviderMismatch",
                "ABC[cached 1]ABC[cached 1]abc[cached 2]abc[cached 2]ABC[cached 3]ABC[cached 3]");
        assertOutputForNamed(
                "customDataProviderMismatch",
                "ABC[cached 3]ABC[cached 3]abc[cached 4]abc[cached 4]ABC[cached 5]ABC[cached 5]");
    }
    
    @SuppressWarnings("boxing")
    @Override
    protected Object createDataModel() {
        return ImmutableMap.<String, Object>of(
                "uc", new CachingUpperCaseDirective(),
                "lc", new CachingLowerCaseDirective(),
                "x", 123);
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

}
