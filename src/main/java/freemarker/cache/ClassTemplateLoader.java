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

package freemarker.cache;

import java.net.URL;

import freemarker.template.utility.StringUtil;

/**
 * A {@link TemplateLoader} that uses streams reachable through 
 * {@link Class#getResourceAsStream(String)} as its source of templates.
 */
public class ClassTemplateLoader extends URLTemplateLoader
{
    private final Class baseClass;
    private final ClassLoader classLoader;
    private final String packagePath;
    
    /**
     * Creates a template loader that will use the {@link Class#getResource(String)}
     * method of its own class to load the resources, and <code>"/"</code> as base path.
     * This means that that template paths will be resolved relatvively the root package
     * of the class hierarchy, so you hardly ever should use this constructor, rather do
     * something like this:<br>
     * {@link #ClassTemplateLoader(Class, String)
     * new ClassTemplateLoader(com.example.myapplication.SomeClass.class, "templates")}
     *
     * <p>If you extend this class, then the extending class will be used to load
     * the resources.
     *
     * <p>Warning: this constructor was malfunctioned prior FreeMarker 2.3.4
     * -- please update FreeMarker if needed.
     *
     * @deprecated confusing constructor, and seldom useful;
     *     use {@link #ClassTemplateLoader(Class, String)} instead.
     */
    public ClassTemplateLoader()
    {
        this(null, true, null, "/");
    }

    /**
     * Creates a template loader that will use the {@link Class#getResource(String)}
     * method of the specified class to load the resources, and <code>""</code> as base
     * path. This means that template paths will be resolved relatively to the class
     * location, that is, relatively to the directory (package) of the class.
     *
     * @param baseClass the class whose
     * {@link Class#getResource(String)} will be used to load the templates.
     *
     * @deprecated it's confusing that the base path is <code>""</code>;
     *     use {@link #ClassTemplateLoader(Class, String)} instead.
     */
    public ClassTemplateLoader(Class baseClass)
    {
        this(baseClass, "");
    }

    /**
     * Creates a template loader that will use the {@link Class#getResource(String)} method
     * of the specified class to load the resources, and the specified base path (absolute or relative).
     *
     * <p>Examples:
     * <ul>
     *   <li>Relative base path (will load from the
     *       <code>com.example.myapplication.templates</code> package):<br>
     *       <code>new ClassTemplateLoader(<br>
     *       com.example.myapplication.SomeClass.class,<br>
     *       "templates")</code>
     *   <li>Absolute base path:<br>
     *       <code>new ClassTemplateLoader(<br>
     *       somepackage.SomeClass.class,<br>
     *       "/com/example/myapplication/templates")</code>
     * </ul>
     *
     * @param baseClass the class whose {@link Class#getResource(String)} method will be used
     *     to load the templates. Be sure that you chose a class whose defining class-loader
     *     sees the templates. This parameter can't be <code>null</code>.
     * @param packagePath the path to the package that contains the templates.
     *     A path that doesn't start with a slash (/) is relative to the
     *     path (package) of the specified class. A path that starts with a slash
     *     is an absolute path starting from the root of the package hierarchy. Path
     *     components should be separated by forward slashes independently of the
     *     separator character used by the underlying operating system.
     *     This parameter can't be <code>null</code>.
     */
    public ClassTemplateLoader(Class baseClass, String packagePath)
    {
        this(baseClass, false, null, packagePath);
    }

    /**
     * Similar to {@link #ClassTemplateLoader(Class, String)}, but instead of a class, it uses a {@link ClassLoader} to
     * load the resources. Because {@link ClassLoader} aren't belonging to any package, a relative {@code packagePath}
     * will mean the same as an absolute.
     *  
     * @since 2.3.22
     */
    public ClassTemplateLoader(ClassLoader classLoader, String packagePath)
    {
        this(null, true, classLoader, packagePath);
    }
    
    private ClassTemplateLoader(Class baseClass, boolean allowNullBaseClass, ClassLoader classLoader, String packagePath)
    {
        if(baseClass == null && !allowNullBaseClass)
        {
            throw new IllegalArgumentException("baseClass == null");
        }
        if(packagePath == null)
        {
            throw new IllegalArgumentException("path == null");
        }
        
        // Either set a non-null baseClass or a non-null classLoader, not both:
        this.baseClass = classLoader == null ? (baseClass == null ? this.getClass() : baseClass) : null;
        if (this.baseClass == null && classLoader == null) {
            throw new IllegalArgumentException("classLoader == null");
        }
        this.classLoader = classLoader;
        
        this.packagePath = canonicalizePrefix(packagePath);
    }
    
    protected URL getURL(String name)
    {
        String fullPath = packagePath + name;
        
        // Block java.net.URLClassLoader exploits:
        if (packagePath.equals("/") && !isSchemeless(fullPath)) {
            return null;
        }
        
        return baseClass != null ? baseClass.getResource(fullPath) : classLoader.getResource(fullPath);
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
     * 
     * @since 2.3.21
     */
   public String toString() {
        return "ClassTemplateLoader("
                + (baseClass != null
                        ? "baseClass=" + baseClass.getName()
                        : "classLoader=" + StringUtil.jQuote(classLoader))
                + ", packagePath=" + StringUtil.jQuote(packagePath) + ")";
    }

}