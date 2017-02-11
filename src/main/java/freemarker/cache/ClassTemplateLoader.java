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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.StringUtil;

/**
 * A {@link TemplateLoader} that can load templates from the "classpath". Naturally, it can load from jar files, or from
 * anywhere where Java can load classes from. Internally, it uses {@link Class#getResource(String)} or
 * {@link ClassLoader#getResource(String)} to load templates.
 */
// TODO
public class ClassTemplateLoader extends URLTemplateLoader {
    
    private final Class<?> resourceLoaderClass;
    private final ClassLoader classLoader;
    private final String basePackagePath;

    /**
     * Creates a template loader that will use the {@link Class#getResource(String)} method of the specified class to
     * load the resources, and the specified base package path (absolute or relative).
     *
     * <p>
     * Examples:
     * <ul>
     * <li>Relative base path (will load from the {@code com.example.myapplication.templates} package):<br>
     * {@code new ClassTemplateLoader(com.example.myapplication.SomeClass.class, "templates")}
     * <li>Absolute base path:<br>
     * {@code new ClassTemplateLoader(somepackage.SomeClass.class, "/com/example/myapplication/templates")}
     * </ul>
     *
     * @param resourceLoaderClass
     *            The class whose {@link Class#getResource(String)} method will be used to load the templates. Be sure
     *            that you chose a class whose defining class-loader sees the templates. This parameter can't be
     *            {@code null}.
     * @param basePackagePath
     *            The package that contains the templates, in path ({@code /}-separated) format. If it doesn't start
     *            with a {@code /} then it's relative to the path (package) of the {@code resourceLoaderClass} class. If
     *            it starts with {@code /} then it's relative to the root of the package hierarchy. Note that path
     *            components should be separated by forward slashes independently of the separator character used by the
     *            underlying operating system. This parameter can't be {@code null}.
     * 
     * @see #ClassTemplateLoader(ClassLoader, String)
     */
    public ClassTemplateLoader(Class<?> resourceLoaderClass, String basePackagePath) {
        this(resourceLoaderClass, false, null, basePackagePath);
    }

    /**
     * Similar to {@link #ClassTemplateLoader(Class, String)}, but instead of {@link Class#getResource(String)} it uses
     * {@link ClassLoader#getResource(String)}. Because a {@link ClassLoader} isn't bound to any Java package, it
     * doesn't mater if the {@code basePackagePath} starts with {@code /} or not, it will be always relative to the root
     * of the package hierarchy
     */
    public ClassTemplateLoader(ClassLoader classLoader, String basePackagePath) {
        this(null, true, classLoader, basePackagePath);
    }

    private ClassTemplateLoader(Class<?> resourceLoaderClass, boolean allowNullResourceLoaderClass,
            ClassLoader classLoader, String basePackagePath) {
        if (!allowNullResourceLoaderClass) {
            NullArgumentException.check("resourceLoaderClass", resourceLoaderClass);
        }
        NullArgumentException.check("basePackagePath", basePackagePath);

        // Either set a non-null resourceLoaderClass or a non-null classLoader, not both:
        this.resourceLoaderClass = classLoader == null ? (resourceLoaderClass == null ? this.getClass()
                : resourceLoaderClass) : null;
        if (this.resourceLoaderClass == null && classLoader == null) {
            throw new NullArgumentException("classLoader");
        }
        this.classLoader = classLoader;

        String canonBasePackagePath = canonicalizePrefix(basePackagePath);
        if (this.classLoader != null && canonBasePackagePath.startsWith("/")) {
            canonBasePackagePath = canonBasePackagePath.substring(1);
        }
        this.basePackagePath = canonBasePackagePath;
    }

    private static boolean isSchemeless(String fullPath) {
        int i = 0;
        int ln = fullPath.length();

        // Skip a single initial /, as things like "/file:/..." might work:
        if (i < ln && fullPath.charAt(i) == '/') i++;

        // Check if there's no ":" earlier than a '/', as the URLClassLoader
        // could interpret that as an URL scheme:
        while (i < ln) {
            char c = fullPath.charAt(i);
            if (c == '/') return true;
            if (c == ':') return false;
            i++;
        }
        return true;
    }

    /**
     * Show class name and some details that are useful in template-not-found errors.
     */
    @Override
    public String toString() {
        return TemplateLoaderUtils.getClassNameForToString(this) + "("
                + (resourceLoaderClass != null
                        ? "resourceLoaderClass=" + resourceLoaderClass.getName()
                        : "classLoader=" + StringUtil.jQuote(classLoader))
                + ", basePackagePath"
                + "="
                + StringUtil.jQuote(basePackagePath)
                + (resourceLoaderClass != null
                        ? (basePackagePath.startsWith("/") ? "" : " /* relatively to resourceLoaderClass pkg */")
                        : ""
                )
                + ")";
    }

    /**
     * See the similar parameter of {@link #ClassTemplateLoader(Class, String)}; {@code null} when other mechanism is
     * used to load the resources.
     */
    public Class<?> getResourceLoaderClass() {
        return resourceLoaderClass;
    }

    /**
     * See the similar parameter of {@link #ClassTemplateLoader(ClassLoader, String)}; {@code null} when other mechanism
     * is used to load the resources.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * See the similar parameter of {@link #ClassTemplateLoader(ClassLoader, String)}; note that this is a normalized
     * version of what was actually passed to the constructor.
     */
    public String getBasePackagePath() {
        return basePackagePath;
    }

    @Override
    protected URL getURL(String name) {
        String fullPath = basePackagePath + name;
    
        // Block java.net.URLClassLoader exploits:
        if (basePackagePath.equals("/") && !isSchemeless(fullPath)) {
            return null;
        }
    
        return resourceLoaderClass != null ? resourceLoaderClass.getResource(fullPath) : classLoader
                .getResource(fullPath);
    }

    @Override
    protected TemplateLoadingResult extractNegativeResult(URLConnection conn) throws IOException {
        return null;
    }

}