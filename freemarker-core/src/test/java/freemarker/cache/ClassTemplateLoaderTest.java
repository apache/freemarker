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

package freemarker.cache;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;

public class ClassTemplateLoaderTest {
    private static final String KNOWN_EXISTING_TEMPLATE_NAME_1 = "1.ftl";
    private static final String KNOWN_EXISTING_TEMPLATE_NAME_1_NON_NORMALIZED = "sub/../1.ftl";
    private static final String KNOWN_EXISTING_TEMPLATE_NAME_2 = "sub/2.ftl";

    private final ClassTemplateLoader tl = new ClassTemplateLoader(ClassTemplateLoaderTest.class, "templates");

    @Test
    public void testCanLoadSomeTemplate() throws IOException, TemplateException {
        assertNotNull(tl.findTemplateSource(KNOWN_EXISTING_TEMPLATE_NAME_1));
        assertNotNull(tl.findTemplateSource(KNOWN_EXISTING_TEMPLATE_NAME_1_NON_NORMALIZED));
        assertNotNull(tl.findTemplateSource(KNOWN_EXISTING_TEMPLATE_NAME_2));

        // Just to be sure it actually works...
        var cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setTemplateLoader(tl);
        Template tGet1 = cfg.getTemplate(KNOWN_EXISTING_TEMPLATE_NAME_1);
        Template tGet2 = cfg.getTemplate(KNOWN_EXISTING_TEMPLATE_NAME_1_NON_NORMALIZED);
        assertSame(tGet1, tGet2); // Because of template caching
        var out = new StringWriter();
        tGet1.process(null, out);
        assertThat(out.toString(), is("1"));
    }

    @Test
    public void testFindReturnsNullWhenNotFound() throws IOException, TemplateException {
        List.of("missing.ftl", "sub/missing.ftl", "sub/./missing.ftl", "sub/../missing.ftl")
                .forEach(name -> forMultipleNameVariants(name,
                        nameVariant -> {
                            try {
                                assertNull(tl.findTemplateSource(nameVariant));
                            } catch (IOException e) {
                                fail(e.getMessage());
                            }
                        })
                );
    }

    @Test
    public void testThrowsMalformedWhenLeavesRoot() {
        List.of("..", "sub/../../1.ftl")
                .forEach(name -> forMultipleNameVariants(name,
                        nameVariant -> {
                            try {
                                tl.findTemplateSource(nameVariant);
                                fail("Expected exception thrown for " + StringUtil.jQuote(nameVariant));
                            } catch (MalformedTemplateNameException e) {
                                // Expected outcome
                                assertThat(e.getTemplateName(), is(nameVariant));
                            } catch (IOException e) {
                                fail("Unexpected exception when getting " + StringUtil.jQuote(nameVariant) + ": " + e);
                            }
                        })
                );
    }

    private void forMultipleNameVariants(String name, Consumer<String> asserter) {
        asserter.accept(name);
        asserter.accept(name.replace('/', '\\'));
    }
}
