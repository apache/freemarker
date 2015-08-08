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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import org.junit.Test;

import freemarker.cache.ByteArrayTemplateLoader;
import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.FirstMatchTemplateConfigurerFactory;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateConfigurerWithTemplateCacheTest {

    private static final String TEXT_WITH_ACCENTS = "pr\u00F3ba";

    @Test
    public void testEncoding() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setDefaultEncoding("iso-8859-1");
        
        ByteArrayTemplateLoader tl = new ByteArrayTemplateLoader();
        tl.putTemplate("utf8.ftl", TEXT_WITH_ACCENTS.getBytes("utf-8"));
        tl.putTemplate("utf16.ftl", TEXT_WITH_ACCENTS.getBytes("utf-16"));
        tl.putTemplate("default.ftl", TEXT_WITH_ACCENTS.getBytes("iso-8859-2"));
        tl.putTemplate("utf8-latin2.ftl",
                ("<#ftl encoding='iso-8859-2'>" + TEXT_WITH_ACCENTS).getBytes("iso-8859-2"));
        tl.putTemplate("default-latin2.ftl",
                ("<#ftl encoding='iso-8859-2'>" + TEXT_WITH_ACCENTS).getBytes("iso-8859-2"));
        cfg.setTemplateLoader(tl);
        
        TemplateConfigurer tcUtf8 = new TemplateConfigurer();
        tcUtf8.setEncoding("utf-8");
        TemplateConfigurer tcUtf16 = new TemplateConfigurer();
        tcUtf16.setEncoding("utf-16");
        cfg.setTemplateConfigurers(
                new FirstMatchTemplateConfigurerFactory(
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*utf8*"), tcUtf8),
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*utf16*"), tcUtf16)
                ).allowNoMatch(true));
        
        {
            Template t = cfg.getTemplate("utf8.ftl");
            assertEquals("utf-8", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf8.ftl", "iso-8859-1");
            assertEquals("utf-8", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf16.ftl");
            assertEquals("utf-16", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl");
            assertEquals("iso-8859-1", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl", "iso-8859-5");
            assertEquals("iso-8859-5", t.getEncoding());
            assertEquals(new String(TEXT_WITH_ACCENTS.getBytes("iso-8859-1"), "iso-8859-5"),
                    getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf8-latin2.ftl");
            assertEquals("iso-8859-2", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default-latin2.ftl");
            assertEquals("iso-8859-2", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
    }
    
    @Test
    public void testLocale() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setLocale(Locale.US);
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("(de).ftl", "${.locale}");
        tl.putTemplate("default.ftl", "${.locale}");
        tl.putTemplate("(de)-fr.ftl",
                ("<#ftl locale='fr_FR'>${.locale}"));
        tl.putTemplate("default-fr.ftl",
                ("<#ftl locale='fr_FR'>${.locale}"));
        cfg.setTemplateLoader(tl);

        TemplateConfigurer tcDe = new TemplateConfigurer();
        tcDe.setLocale(Locale.GERMANY);
        cfg.setTemplateConfigurers(new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*(de)*"), tcDe));
        
        {
            Template t = cfg.getTemplate("(de).ftl");
            assertEquals(Locale.GERMANY, t.getLocale());
            assertEquals("de_DE", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("(de).ftl", Locale.ITALY);
            assertEquals(Locale.GERMANY, t.getLocale());
            assertEquals("de_DE", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl");
            assertEquals(Locale.US, t.getLocale());
            assertEquals("en_US", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl", Locale.ITALY);
            assertEquals(Locale.ITALY, t.getLocale());
            assertEquals("it_IT", getTemplateOutput(t));
        }
    }
    
    private String getTemplateOutput(Template t) throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        t.process(null, sw);
        return sw.toString();
    }

}
