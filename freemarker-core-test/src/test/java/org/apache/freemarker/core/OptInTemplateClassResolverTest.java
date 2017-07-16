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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.freemarker.core.util.OptInTemplateClassResolver;
import org.apache.freemarker.test.TestConfigurationBuilder;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

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
    

    public void testOptIn() throws TemplateException {
        Template dummyTemp = Template.createPlainTextTemplate("foo.ftl", "",
                new TestConfigurationBuilder().build());

        assertEquals(String.class, resolver.resolve("java.lang.String", null, dummyTemp));
        assertEquals(Integer.class, resolver.resolve("java.lang.Integer", null, dummyTemp));
        try {
            resolver.resolve("java.lang.Long", null, dummyTemp);
            fail();
        } catch (TemplateException e) {
            // Expected
        }
    }

    public void testTrusted() throws TemplateException {
        Configuration cfg = new TestConfigurationBuilder().build();

        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.createPlainTextTemplate("lib/foo.ftl", "", cfg)));
        assertEquals(String.class, resolver.resolve("java.lang.String", null,
                Template.createPlainTextTemplate("lib/foo.ftl", "", cfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.createPlainTextTemplate("/lib/foo.ftl", "", cfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.createPlainTextTemplate("include/foo.ftl", "", cfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.createPlainTextTemplate("trusted.ftl", "", cfg)));
        assertEquals(Long.class, resolver.resolve("java.lang.Long", null,
                Template.createPlainTextTemplate("/trusted.ftl", "", cfg)));
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
            // Expected
        }
        try {
            testTrusted_checkFails("lib/foo..ftl");
            fail();
        } catch (AssertionFailedError e) {
            // Expected
        }
        try {
            testTrusted_checkFails("lib/%2e/foo.ftl");
            fail();
        } catch (AssertionFailedError e) {
            // Expected
        }
    }
    
    public void testTrusted_checkFails(String templateName) {
        try {
            resolver.resolve("java.lang.Long", null,
                    Template.createPlainTextTemplate(templateName, "",
                            new TestConfigurationBuilder().build()));
            fail();
        } catch (TemplateException e) {
            // Expected
        }
    }
    
    public void testSettingParser() throws Exception {
        {
            Configuration cfg = new TestConfigurationBuilder()
                    .setting(
                            "newBuiltinClassResolver",
                            "trustedTemplates: foo.ftl, \"lib/*\"")
                    .build();

            TemplateClassResolver res = cfg.getNewBuiltinClassResolver();
            assertEquals(String.class, res.resolve("java.lang.String", null,
                    Template.createPlainTextTemplate("foo.ftl", "", cfg)));
            assertEquals(String.class, res.resolve("java.lang.String", null,
                    Template.createPlainTextTemplate("lib/bar.ftl", "", cfg)));
            try {
                res.resolve("java.lang.String", null,
                        Template.createPlainTextTemplate("bar.ftl", "", cfg));
                fail();
            } catch (TemplateException e) {
                // Expected
            }
        }

        {
            Configuration cfg = new TestConfigurationBuilder()
                    .setting(
                            "newBuiltinClassResolver",
                            "allowedClasses: java.lang.String, java.lang.Integer")
                    .build();

            TemplateClassResolver res = cfg.getNewBuiltinClassResolver();
            assertEquals(String.class, res.resolve("java.lang.String", null,
                    Template.createPlainTextTemplate("foo.ftl", "", cfg)));
            assertEquals(Integer.class, res.resolve("java.lang.Integer", null,
                    Template.createPlainTextTemplate("foo.ftl", "", cfg)));
            try {
                res.resolve("java.lang.Long", null,
                        Template.createPlainTextTemplate("foo.ftl", "", cfg));
                fail();
            } catch (TemplateException e) {
                // good
            }
        }

        {
            Configuration cfg = new TestConfigurationBuilder()
                    .setting(
                            "newBuiltinClassResolver",
                            "trustedTemplates: foo.ftl, 'lib/*', allowedClasses: 'java.lang.String',"
                            + " java.lang.Integer")
                    .build();
            TemplateClassResolver res = cfg.getNewBuiltinClassResolver();
            assertEquals(String.class, res.resolve("java.lang.String", null,
                    Template.createPlainTextTemplate("x.ftl", "", cfg)));
            assertEquals(Integer.class, res.resolve("java.lang.Integer", null,
                    Template.createPlainTextTemplate("x.ftl", "", cfg)));
            try {
                res.resolve("java.lang.Long", null,
                        Template.createPlainTextTemplate("x.ftl", "", cfg));
                fail();
            } catch (TemplateException e) {
                // Expected
            }
            assertEquals(Long.class, res.resolve("java.lang.Long", null,
                    Template.createPlainTextTemplate("foo.ftl", "", cfg)));
            assertEquals(Long.class, res.resolve("java.lang.Long", null,
                    Template.createPlainTextTemplate("lib/bar.ftl", "", cfg)));
            try {
                res.resolve("java.lang.Long", null,
                        Template.createPlainTextTemplate("x.ftl", "", cfg));
                fail();
            } catch (TemplateException e) {
                // Expected
            }
        }
        
        try {
            new TestConfigurationBuilder().setSetting("newBuiltinClassResolver", "wrong: foo");
            fail();
        } catch (ConfigurationException e) {
            // Expected
        }

        {
            Configuration cfg = new TestConfigurationBuilder()
                    .setting(
                            "newBuiltinClassResolver",
                            "\"allowedClasses\"  :  java.lang.String  ,  'trustedTemplates' :\"lib:*\"")
                    .build();
            TemplateClassResolver res = cfg.getNewBuiltinClassResolver();
            assertEquals(String.class, res.resolve("java.lang.String", null,
                    Template.createPlainTextTemplate("x.ftl", "", cfg)));
            try {
                res.resolve("java.lang.Long", null,
                        Template.createPlainTextTemplate("x.ftl", "", cfg));
                fail();
            } catch (TemplateException e) {
                // Expected
            }
            assertEquals(Long.class, res.resolve("java.lang.Long", null,
                    Template.createPlainTextTemplate("lib:bar.ftl", "", cfg)));
        }
    }
    
}
