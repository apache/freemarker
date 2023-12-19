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

package org.apache.freemarker.test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.apache.freemarker.core.*;
import org.apache.freemarker.core.TemplateConfiguration.Builder;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtils;
import org.junit.Ignore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

/**
 * Superclass of JUnit tests that process templates.
 */
@Ignore
public abstract class TemplateTest {
    
    private Configuration configuration;
    private boolean dataModelCreated;
    private Object dataModel;
    private Map<String, String> addedTemplates = new HashMap<>();
    private LinkedList<TemplateConfiguration> tcStack = new LinkedList<>();

    /**
     * Gets the {@link Configuration} used, automatically creating and setting if it wasn't yet. Automatic creation
     * happens via {@link #newConfigurationBuilder()}.
     */
    protected final Configuration getConfiguration() {
        if (configuration == null) {
            try {
                setConfiguration(newConfigurationBuilder());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create configuration", e);
            }
        }
        return configuration;
    }

    /**
     * Set the {@link Configuration} used.
     *
     * @param configuration The {@link Configuration} used from now on; not {@code null}. Usually built with
     * {@link #newConfigurationBuilder()}.
     *
     * @see #pushNamelessTemplateConfiguraitonSettings(TemplateConfiguration)
     */
    protected final void setConfiguration(Configuration configuration) {
        _NullArgumentException.check("configuration", configuration);
        if (this.configuration == configuration) {
            return;
        }

        this.configuration = configuration;
        afterConfigurationSet();
    }

    /**
     * Convenience overload, that calls {@link #setConfiguration(Configuration)} after calling
     * with the result of {@link Configuration.ExtendableBuilder#build()}.
     */
    protected final void setConfiguration(Configuration.ExtendableBuilder<?> configurationBuilder) {
        setConfiguration(configurationBuilder.build());
    }

    protected final void setConfiguration(Consumer<Configuration.ExtendableBuilder<?>> cfgBuilderAdjuster) {
        Configuration.ExtendableBuilder<?> configBuilder = newConfigurationBuilder();
        cfgBuilderAdjuster.accept(configBuilder);
        setConfiguration(configBuilder.build());
    }

    protected void assertOutput(String ftl, String expectedOut) throws IOException, TemplateException {
        assertOutput(createTemplate(ftl), expectedOut, false);
    }

    private Template createTemplate(String ftl) throws IOException {
        return new Template(null, ftl, getConfiguration(), getNamelessTemplateConfiguration());
    }

    protected void assertOutputForNamed(String name, String expectedOut) throws IOException, TemplateException {
        assertOutput(getConfiguration().getTemplate(name), expectedOut, false);
    }

    @SuppressFBWarnings(value="UI_INHERITANCE_UNSAFE_GETRESOURCE", justification="By design relative to subclass")
    protected void assertOutputForNamed(String name) throws IOException, TemplateException {
        String expectedOut;
        {
            String resName = name + ".out";
            InputStream in = getClass().getResourceAsStream(resName);
            if (in == null) {
                throw new IOException("Reference output resource not found: " + getClass() + ", " + resName);
            }
            try {
                expectedOut = TestUtils.removeTxtCopyrightComment(IOUtils.toString(in, StandardCharsets.UTF_8.name()));
            } finally {
                in.close();
            }
        }
        assertOutput(getConfiguration().getTemplate(name), expectedOut, true);
    }

    protected void assertOutput(Template t, String expectedOut) throws TemplateException, IOException {
        assertOutput(t, expectedOut, false);
    }
    
    protected void assertOutput(Template t, String expectedOut, boolean normalizeNewlines)
            throws TemplateException, IOException {
        String actualOut = getOutput(t);
        
        if (normalizeNewlines) {
            expectedOut = normalizeNewLines(expectedOut);
            actualOut = normalizeNewLines(actualOut);
        }
        assertEquals(expectedOut, actualOut);
    }

    protected String getOutput(String ftl) throws IOException, TemplateException {
        return getOutput(createTemplate(ftl));
    }
    
    protected String getOutput(Template t) throws TemplateException, IOException {
        StringWriter out = new StringWriter();
        t.process(getDataModel(), new FilterWriter(out) {
            private boolean closed;

            @Override
            public void write(int c) throws IOException {
                checkNotClosed();
                super.write(c);
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                checkNotClosed();
                super.write(cbuf, off, len);
            }

            @Override
            public void write(String str, int off, int len) throws IOException {
                checkNotClosed();
                super.write(str, off, len);
            }

            @Override
            public void close() throws IOException {
                super.close();
                closed = true;
            }
            
            private void checkNotClosed() throws IOException {
                if (closed) {
                    throw new IOException("Writer is already closed");
                }
            }
        });
        return out.toString();
    }

    /**
     * Same as {@link #newConfigurationBuilderBeforeSetup(Version)} with {@code null} parameter.
     */
    protected final Configuration.ExtendableBuilder<?> newConfigurationBuilder() {
        return newConfigurationBuilder(null);
    }

    /**
     * Return a new {@link TestConfigurationBuilder} that you can pass to {@link #setConfiguration(Configuration)}.
     * This is also called automatically when {@link #getConfiguration()} is called for the first time, and
     * #setConfiguration(Configuration)} wasn't called before.
     * The instance will be created with {@link #newConfigurationBuilderBeforeSetup(Version)}, and then set up with
     * {@link #setupConfigurationBuilder(Configuration.ExtendableBuilder)}.
     *
     * @param incompatibleImprovements Can be {@code null}, in which case a default value is used (normally the
     *                                 lowest supported value).
     */
    protected final Configuration.ExtendableBuilder<?> newConfigurationBuilder(Version incompatibleImprovements) {
        Configuration.ExtendableBuilder<?> cb = newConfigurationBuilderBeforeSetup(incompatibleImprovements);
        setupConfigurationBuilder(cb);
        return cb;
    }

    /**
     * Override this only if the {@link TestConfigurationBuilder} <em>class</em> is not appropriate for the test.
     * Otherwise you probably should override {@link #setupConfigurationBuilder(Configuration.ExtendableBuilder)} instead.
     *
     * @param incompatibleImprovements {@code null} if wasn't explicitly specified in the test.
     */
    protected Configuration.ExtendableBuilder<?> newConfigurationBuilderBeforeSetup(Version incompatibleImprovements) {
        return new TestConfigurationBuilder(incompatibleImprovements, this.getClass());
    }

    /**
     * Override this if you want change the configuration settings for all tests in a test class.
     * The default implementation in {@link TemplateTest} does nothing.
     */
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        // No op
    }

    private void afterConfigurationSet() {
        ensureAddedTemplatesPresent();
        addCommonTemplates();
    }

    private void ensureAddedTemplatesPresent() {
        for (Map.Entry<String, String> ent : addedTemplates.entrySet()) {
            addTemplate(ent.getKey(), ent.getValue());
        }
    }

    protected void addCommonTemplates() {
        //
    }

    protected Object getDataModel() {
        if (!dataModelCreated) {
            dataModel = createDataModel();
            dataModelCreated = true;
        }
        return dataModel;
    }
    
    protected Object createDataModel() {
        return null;
    }
    
    @SuppressWarnings("boxing")
    protected Map<String, Object> createCommonTestValuesDataModel() {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("map", Collections.singletonMap("key", "value"));
        dataModel.put("list", Collections.singletonList("item"));
        dataModel.put("s", "text");
        dataModel.put("n", 1);
        dataModel.put("b", true);
        dataModel.put("bean", new TestBean());
        return dataModel;
    }

    protected void addTemplate(String name, String content) {
        Configuration cfg = getConfiguration();
        TemplateLoader tl = cfg.getTemplateLoader();
        ByteArrayTemplateLoader btl;
        btl = extractByteArrayTemplateLoader(tl);
        btl.putTemplate(name, content.getBytes(StandardCharsets.UTF_8));
        addedTemplates.put(name, content);
    }

    private ByteArrayTemplateLoader extractByteArrayTemplateLoader(TemplateLoader tl) {
        if (tl instanceof MultiTemplateLoader) {
            MultiTemplateLoader mtl = (MultiTemplateLoader) tl;
            for (int i = 0; i < mtl.getTemplateLoaderCount(); i++) {
                TemplateLoader tli = mtl.getTemplateLoader(i);
                if (tli instanceof ByteArrayTemplateLoader) {
                    return (ByteArrayTemplateLoader) tli;
                }
            }
            throw new IllegalStateException(
                    "The template loader was a MultiTemplateLoader that didn't contain ByteArrayTemplateLoader: "
                            + tl);
        } else if (tl instanceof ByteArrayTemplateLoader) {
            return (ByteArrayTemplateLoader) tl;
        } else if (tl == null) {
            throw new IllegalStateException("The templateLoader was null in the configuration");
        } else {
            throw new IllegalStateException(
                    "The template loader was set to a non-ByteArrayTemplateLoader non-MultiTemplateLoader: "
                            + tl);
        }
    }

    protected void addToDataModel(String name, Object value) {
        Object dm = getDataModel();
        if (dm == null) {
            dm = new HashMap<String, Object>();
            dataModel = dm;
        }
        if (dm instanceof Map) {
            ((Map) dm).put(name, value);
        } else {
            throw new IllegalStateException("Can't add to non-Map data-model: " + dm);
        }
    }

    protected Properties loadPropertiesFile(String name) throws IOException {
        Properties props = new Properties();
        Class<? extends TemplateTest> baseClass = getClass();
        InputStream in = baseClass.getResourceAsStream(name);
        if (in == null) {
            throw new FileNotFoundException(
                    "Classpath resource not found: baseClass=" + baseClass.getName() + ", name=" + name);
        }
        try {
            props.load(in);
        } finally {
            in.close();
        }
        return props;
    }

    protected Throwable assertErrorContains(String ftl, String... expectedSubstrings) {
        return assertErrorContains(null, ftl, null, expectedSubstrings);
    }

    protected Throwable assertErrorContains(String ftl, Class<? extends Throwable> exceptionClass,
            String... expectedSubstrings) {
        return assertErrorContains(null, ftl, exceptionClass, expectedSubstrings);
    }

    protected void assertErrorContainsForNamed(String name, String... expectedSubstrings) {
        assertErrorContains(name, null, null, expectedSubstrings);
    }

    protected void assertErrorContainsForNamed(String name, Class<? extends Throwable> exceptionClass,
            String... expectedSubstrings) {
        assertErrorContains(name, null, exceptionClass, expectedSubstrings);
    }

    private Throwable assertErrorContains(String name, String ftl, Class<? extends Throwable> exceptionClass,
            String... expectedSubstrings) {
        try {
            Template t;
            if (ftl == null) {
                t = getConfiguration().getTemplate(name);
            } else {
                t = new Template("adhoc", ftl, getConfiguration(), getNamelessTemplateConfiguration());
            }
            t.process(getDataModel(), new StringWriter());
            fail("The template had to fail");
            return null;
        } catch (TemplateException e) {
            if (exceptionClass != null) {
                assertThat(e, instanceOf(exceptionClass));
            }
            assertContainsAll(e.getMessageWithoutStackTop(), expectedSubstrings);
            return e;
        } catch (ParseException e) {
            if (exceptionClass != null) {
                assertThat(e, instanceOf(exceptionClass));
            }
            assertContainsAll(e.getEditorMessage(), expectedSubstrings);
            return e;
        } catch (Exception e) {
            if (exceptionClass != null) {
                assertThat(e, instanceOf(exceptionClass));
                return e;
            } else {
                throw new RuntimeException("Unexpected exception class: " + e.getClass().getName(), e);
            }
        }
    }
    
    private void assertContainsAll(String msg, String... expectedSubstrings) {
        for (String needle: expectedSubstrings) {
            if (needle.startsWith("\\!")) {
                String netNeedle = needle.substring(2); 
                if (msg.contains(netNeedle)) {
                    fail("The message shouldn't contain substring " + _StringUtils.jQuote(netNeedle) + ":\n" + msg);
                }
            } else if (!msg.contains(needle)) {
                fail("The message didn't contain substring " + _StringUtils.jQuote(needle) + ":\n" + msg);
            }
        }
    }
    
    private String normalizeNewLines(String s) {
        return _StringUtils.replace(s, "\r\n", "\n").replace('\r', '\n');
    }
    
    /**
     * Adds a {@link TemplateConfiguration} that will be used for further templates tested, but not for templates that
     * are tested with the "OfNamed" variants (which are coming from a {@link TemplateLoader}). If a
     * {@link TemplateConfiguration} was added earlier, this new one will be merged into it.
     * 
     * @see #setConfiguration(Configuration)
     */
    protected void pushNamelessTemplateConfiguraitonSettings(TemplateConfiguration tc) {
        TemplateConfiguration lastTC = tcStack.poll();
        TemplateConfiguration mergedTC;
        if (lastTC != null) {
            Builder mergedTCB = new TemplateConfiguration.Builder();
            mergedTCB.merge(lastTC);
            mergedTCB.merge(tc);
            mergedTC = mergedTCB.build();
        } else {
            mergedTC = tc;
        }
        tcStack.push(mergedTC);
    }

    /**
     * Undoes the last {@link #pushNamelessTemplateConfiguraitonSettings(TemplateConfiguration)}.
     */
    protected void popNamelessTemplateConfiguraitonSettings() {
        tcStack.pop();
    }
    
    private TemplateConfiguration getNamelessTemplateConfiguration() {
        return tcStack.peek();
    }

    public static class TestBean {
        private int x;
        private boolean b;
        
        public int getX() {
            return x;
        }
        public void setX(int x) {
            this.x = x;
        }
        public boolean isB() {
            return b;
        }
        public void setB(boolean b) {
            this.b = b;
        }

        public int intM() {
            return 1;
        }

        public int intMP(int x) {
            return x;
        }
        
        public void voidM() {
            
        }
        
    }

}
