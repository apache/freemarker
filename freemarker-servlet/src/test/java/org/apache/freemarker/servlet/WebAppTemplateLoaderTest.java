package org.apache.freemarker.servlet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.ResourceUtils;

public class WebAppTemplateLoaderTest {

    private static final Logger LOG = LoggerFactory.getLogger(WebAppTemplateLoader.class);

    @Test
    public void testTemplateFound() throws Exception {
        assertEquals("foo", createConfiguration().getTemplate("test.ftl").toString());
    }

    @Test
    public void testTemplateNotFound() throws IOException {
        try {
            createConfiguration().getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            LOG.debug("Expected result", e);
            String errMsg = e.getMessage();
            assertThat(errMsg, containsString("WebAppTemplateLoader"));
            assertThat(errMsg, containsString("MyApp"));
            assertThat(errMsg, containsString("WEB-INF/templates"));
        }
    }

    private Configuration createConfiguration() {
        MockServletContext servletContext = new MockServletContext(
                ResourceUtils.CLASSPATH_URL_PREFIX
                        + "org/apache/freemarker/servlet/webapptemplateloadertest");
        servletContext.setServletContextName("MyApp");
        WebAppTemplateLoader tl = new WebAppTemplateLoader(servletContext, "WEB-INF/templates");
        return new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build();
    }

}
