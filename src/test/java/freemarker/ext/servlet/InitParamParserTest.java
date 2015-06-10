package freemarker.ext.servlet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;

public class InitParamParserTest {

    @Test
    public void testFindTemplatePathSettingAssignmentsStart() {
        assertEquals(0, InitParamParser.findTemplatePathSettingAssignmentsStart("?settings()"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings()"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=1, y=2)"));
        assertEquals(2, InitParamParser.findTemplatePathSettingAssignmentsStart("x ? settings ( x=1, y=2 ) "));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=f(), y=g())"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=\"(\", y='(')"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=\"(\\\"\", y='(\\'')"));

        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart(""));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("settings"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("settings()"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("foo?/settings(x=1)"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings()x=1)"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings((x=1)"));

        try {
            assertEquals(0, InitParamParser.findTemplatePathSettingAssignmentsStart("x?setting(x = 1)"));
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("\"setting\""));
        }
    }

    @Test
    public void testCreateTemplateLoader() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);

        {
            ClassTemplateLoader ctl = (ClassTemplateLoader) InitParamParser.createTemplateLoader(
                    "classpath:templates",
                    cfg, this.getClass(), null);
            assertEquals("templates/", ctl.getBasePackagePath());
            assertNull(ctl.getURLConnectionUsesCaches());
        }

        {
            ClassTemplateLoader ctl = (ClassTemplateLoader) InitParamParser.createTemplateLoader(
                    "classpath:templates?settings(URLConnectionUsesCaches=false)",
                    cfg, this.getClass(), null);
            assertEquals("templates/", ctl.getBasePackagePath());
            assertEquals(Boolean.FALSE, ctl.getURLConnectionUsesCaches());
        }

        {
            MultiTemplateLoader mtl = (MultiTemplateLoader) InitParamParser.createTemplateLoader(
                    "["
                            + "classpath:templates?settings(URLConnectionUsesCaches=false), "
                            + "classpath:foo/templates?settings(URLConnectionUsesCaches=true)"
                            + "]",
                    cfg, this.getClass(), null);
            assertEquals(Boolean.FALSE, ((URLTemplateLoader) mtl.getTemplateLoader(0)).getURLConnectionUsesCaches());
            assertEquals(Boolean.TRUE, ((URLTemplateLoader) mtl.getTemplateLoader(1)).getURLConnectionUsesCaches());
        }

    }

}
