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

/**
 * A {@link TemplateLoader} that uses streams reachable through 
 * {@link Class#getResourceAsStream(String)} as its source of templates.
 */
public class ClassTemplateLoader extends URLTemplateLoader
{
    private Class loaderClass;
    private String path;
    
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
        setFields(this.getClass(), "/");
    }

    /**
     * Creates a template loader that will use the {@link Class#getResource(String)}
     * method of the specified class to load the resources, and <code>""</code> as base
     * path. This means that template paths will be resolved relatively to the class
     * location, that is, relatively to the directory (package) of the class.
     *
     * @param loaderClass the class whose
     * {@link Class#getResource(String)} will be used to load the templates.
     *
     * @deprecated it's confusing that the base path is <code>""</code>;
     *     use {@link #ClassTemplateLoader(Class, String)} instead.
     */
    public ClassTemplateLoader(Class loaderClass)
    {
        setFields(loaderClass, "");
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
     * @param loaderClass the class whose {@link Class#getResource(String)} method will be used
     *     to load the templates. Be sure that you chose a class whose defining class-loader
     *     sees the templates. This parameter can't be <code>null</code>.
     * @param path the base path to template resources.
     *     A path that doesn't start with a slash (/) is relative to the
     *     path (package) of the specified class. A path that starts with a slash
     *     is an absolute path starting from the root of the package hierarchy. Path
     *     components should be separated by forward slashes independently of the
     *     separator character used by the underlying operating system.
     *     This parameter can't be <code>null</code>.
     */
    public ClassTemplateLoader(Class loaderClass, String path)
    {
        setFields(loaderClass, path);
    }

    protected URL getURL(String name)
    {
        String fullPath = path + name;
        
        // Block java.net.URLClassLoader exploits:
        if (path.equals("/") && !isSchemeless(fullPath)) {
            return null;
        }
        
        return loaderClass.getResource(fullPath);
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

    private void setFields(Class loaderClass, String path) {
        if(loaderClass == null)
        {
            throw new IllegalArgumentException("loaderClass == null");
        }
        if(path == null)
        {
            throw new IllegalArgumentException("path == null");
        }
        this.loaderClass = loaderClass;
        this.path = canonicalizePrefix(path);
    }

}