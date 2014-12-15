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
        tl.putTemplate("t1", "<@uc>abc</@uc> <@uc>x=${x}</@uc> <@uc>ab<#-- -->c</@uc>");
        cfg.setTemplateLoader(tl);
    }
    
    @Test
    public void testCustomData() throws IOException, TemplateException {
        setConfiguration(cfg);
        for (int i = 0; i < 3; i++) {
            assertOutputForNamed("t1", "ABC[cached 1] X=123 ABC[cached 2]");
        }
    }

    @SuppressWarnings("boxing")
    @Override
    protected Object createDataModel() {
        return ImmutableMap.<String, Object>of(
                "uc", new CachingUpperCaseDirective(),
                "x", 123);
    }

    private static final class CachingUpperCaseDirective implements TemplateDirectiveModel {

        private static final AtomicInteger bodyRenderingCount = new AtomicInteger();

        public void execute(Environment env, Map params, TemplateModel[] loopVars, final TemplateDirectiveBody body)
                throws TemplateException, IOException {
            if (body == null) {
                return;
            }
            
            final String convertedOutput;

            final DirectiveCallPlace callPlace = env.getCurrentDirectiveCallPlace();
            if (callPlace.isNestedOutputCacheable()) {
                try {
                    convertedOutput = (String) callPlace.getOrCreateCustomData(
                            CachingUpperCaseDirective.class, new ObjectFactory/* <String> */() {

                                public Object createObject() throws Exception {
                                    return getConvertedOutput(body)
                                            + "[cached " + bodyRenderingCount.incrementAndGet() + "]";
                                }

                            });
                } catch (CallPlaceCustomDataInitializationException e) {
                    throw new TemplateModelException("Failed to pre-render nested content", e);
                }
            } else {
                convertedOutput = getConvertedOutput(body);
            }

            env.getOut().write(convertedOutput);
        }

        private String getConvertedOutput(final TemplateDirectiveBody body) throws TemplateException,
                IOException {
            StringWriter sw = new StringWriter();
            body.render(sw);
            return sw.toString().toUpperCase();
        }

    }

}
