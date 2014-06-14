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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.ObjectConstructor;

public class OptInTemplateClassResolverTest extends TestCase {

    public OptInTemplateClassResolverTest(String name) {
        super(name);
    }
    
    private static final Set ALLOWED_CLASSES = new HashSet();
    static {
        ALLOWED_CLASSES.add("java.lang.String");
        ALLOWED_CLASSES.add("java.lang.Integer");
    }
    
    private static final List TRUSTED_TEMPLATES = new ArrayList();
    static {
        TRUSTED_TEMPLATES.add("lib/*");
        TRUSTED_TEMPLATES.add("/include/*");
        TRUSTED_TEMPLATES.add("trusted.ftl");
    }
    
    private OptInTemplateClassResolver resolver = new OptInTemplateClassResolver(
            ALLOWED_CLASSES, TRUSTED_TEMPLATES);
    
    private Configuration dummyCfg = new Configuration();
    private Template dummyTemp = Template.getPlainTextTemplate("foo.ftl", "", dummyCfg);
    
    public void testOptIn() throws TemplateException {
        assertEquals(String.class, resolver.resolve("java.lang.String", null, dummyTemp));
        assertEquals(Integer.class, resolver.resolve("java.lang.Integer", null, dummyTemp));
        try {
            resolver.resolve("java.lang.Long", null, dummyTemp);
            fail();
        } catch (TemplateException e) {
            // good
        }
    }

    public void testTrusted() throws TemplateException {
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("lib/foo.ftl", "", dummyCfg)));
        assertEquals(String.class, resolver.resolve("java.lang.String", null,
                Template.getPlainTextTemplate("lib/foo.ftl", "", dummyCfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("/lib/foo.ftl", "", dummyCfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("include/foo.ftl", "", dummyCfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("trusted.ftl", "", dummyCfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("/trusted.ftl", "", dummyCfg)));
        try {
            assertEquals(Long.class, resolver.resolve(ObjectConstructor.class.getName(), null,
                    Template.getPlainTextTemplate("trusted.ftl", "", dummyCfg)));
            fail();
        } catch (TemplateException e) {
            // good
        }
    }

    public void testCraftedTrusted() throws TemplateException {
        testTrusted_checkFails("lib/../foo.ftl");
        testTrusted_checkFails("lib\\..\\foo.ftl");
        testTrusted_checkFails("lib\\../foo.ftl");
        testTrusted_checkFails("lib/..\\foo.ftl");
        testTrusted_checkFails("lib/..");
        testTrusted_checkFails("lib%2f%2E%2e%5cfoo.ftl");
        testTrusted_checkFails("/lib%5C%.%2e%2Efoo.ftl");
        
        try {
            testTrusted_checkFails("lib/./foo.ftl");
            fail();
        } catch (AssertionFailedError e) {
            // good
        }
        try {
            testTrusted_checkFails("lib/foo..ftl");
            fail();
        } catch (AssertionFailedError e) {
            // good
        }
        try {
            testTrusted_checkFails("lib/%2e/foo.ftl");
            fail();
        } catch (AssertionFailedError e) {
            // good
        }
    }
    
    public void testTrusted_checkFails(String templateName) {
        try {
            resolver.resolve("java.lang.Long", null,
                    Template.getPlainTextTemplate(templateName, "", dummyCfg));
            fail();
        } catch (TemplateException e) {
            // good
        }
    }
    
    public void testSettingParser() throws TemplateException {
        Configuration cfg = new Configuration();
        
        cfg.setSetting("new_builtin_class_resolver",
                "trusted_templates: foo.ftl, \"lib/*\"");
        TemplateClassResolver res = cfg.getNewBuiltinClassResolver();
        assertEquals(String.class, res.resolve("java.lang.String", null,
                Template.getPlainTextTemplate("foo.ftl", "", cfg)));
        assertEquals(String.class, res.resolve("java.lang.String", null,
                Template.getPlainTextTemplate("lib/bar.ftl", "", cfg)));
        try {
            res.resolve("java.lang.String", null,
                    Template.getPlainTextTemplate("bar.ftl", "", cfg));
            fail();
        } catch (TemplateException e) {
            // good
        }

        cfg.setSetting("new_builtin_class_resolver",
                "allowed_classes: java.lang.String, java.lang.Integer");
        res = cfg.getNewBuiltinClassResolver();
        assertEquals(String.class, res.resolve("java.lang.String", null,
                Template.getPlainTextTemplate("foo.ftl", "", cfg)));
        assertEquals(Integer.class, res.resolve("java.lang.Integer", null,
                Template.getPlainTextTemplate("foo.ftl", "", cfg)));
        try {
            res.resolve("java.lang.Long", null,
                    Template.getPlainTextTemplate("foo.ftl", "", cfg));
            fail();
        } catch (TemplateException e) {
            // good
        }

        cfg.setSetting("new_builtin_class_resolver",
                "trusted_templates: foo.ftl, 'lib/*', " +
                "allowed_classes: 'java.lang.String', java.lang.Integer");
        res = cfg.getNewBuiltinClassResolver();
        assertEquals(String.class, res.resolve("java.lang.String", null,
                Template.getPlainTextTemplate("x.ftl", "", cfg)));
        assertEquals(Integer.class, res.resolve("java.lang.Integer", null,
                Template.getPlainTextTemplate("x.ftl", "", cfg)));
        try {
            res.resolve("java.lang.Long", null,
                    Template.getPlainTextTemplate("x.ftl", "", cfg));
            fail();
        } catch (TemplateException e) {
            // good
        }
        assertEquals(Long.class, res.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("foo.ftl", "", cfg)));
        assertEquals(Long.class, res.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("lib/bar.ftl", "", cfg)));
        try {
            res.resolve("java.lang.Long", null,
                    Template.getPlainTextTemplate("x.ftl", "", cfg));
            fail();
        } catch (TemplateException e) {
            // good
        }
        
        try {
            cfg.setSetting("new_builtin_class_resolver", "wrong: foo");
            fail();
        } catch (TemplateException e) {
            // good
        }
        
        cfg.setSetting("new_builtin_class_resolver",
                "\"allowed_classes\"  :  java.lang.String  ,  " +
                "'trusted_templates' :\"lib:*\"");
        res = cfg.getNewBuiltinClassResolver();
        assertEquals(String.class, res.resolve("java.lang.String", null,
                Template.getPlainTextTemplate("x.ftl", "", cfg)));
        try {
            res.resolve("java.lang.Long", null,
                    Template.getPlainTextTemplate("x.ftl", "", cfg));
            fail();
        } catch (TemplateException e) {
            // good
        }
        assertEquals(Long.class, res.resolve("java.lang.Long", null,
                Template.getPlainTextTemplate("lib:bar.ftl", "", cfg)));
    }
    
}
