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

package org.apache.freemarker.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.MutableProcessingConfiguration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateClassResolver;
import org.apache.freemarker.core.TemplateException;

/**
 * A {@link TemplateClassResolver} that resolves only the classes whose name
 * was specified in the constructor.
 */
public class OptInTemplateClassResolver implements TemplateClassResolver {
    
    private final Set/*<String>*/ allowedClasses;
    private final List/*<String>*/ trustedTemplatePrefixes;
    private final Set/*<String>*/ trustedTemplateNames;
    
    /**
     * Creates a new instance. 
     *
     * @param allowedClasses the {@link Set} of {@link String}-s that contains
     *     the full-qualified names of the allowed classes.
     *     Can be <code>null</code> (means not class is allowed).
     * @param trustedTemplates the {@link List} of {@link String}-s that contains
     *     template names (i.e., template root directory relative paths)
     *     and prefix patterns (like <code>"include/*"</code>) of templates
     *     for which {@link TemplateClassResolver#UNRESTRICTED} will be
     *     used (which is not as safe as {@link OptInTemplateClassResolver}).
     *     The list items need not start with <code>"/"</code> (if they are, it
     *     will be removed). List items ending with <code>"*"</code> are treated
     *     as prefixes (i.e. <code>"foo*"</code> matches <code>"foobar"</code>,
     *     <code>"foo/bar/baaz"</code>, <code>"foowhatever/bar/baaz"</code>,
     *     etc.). The <code>"*"</code> has no special meaning anywhere else.
     *     The matched template name is the name (template root directory
     *     relative path) of the template that directly (lexically) contains the
     *     operation (like <code>?new</code>) that wants to get the class. Thus,
     *     if a trusted template includes a non-trusted template, the
     *     <code>allowedClasses</code> restriction will apply in the included
     *     template.
     *     This parameter can be <code>null</code> (means no trusted templates).
     */
    public OptInTemplateClassResolver(
            Set allowedClasses, List<String> trustedTemplates) {
        this.allowedClasses = allowedClasses != null ? allowedClasses : Collections.EMPTY_SET;
        if (trustedTemplates != null) {
            trustedTemplateNames = new HashSet();
            trustedTemplatePrefixes = new ArrayList();
            
            Iterator<String> it = trustedTemplates.iterator();
            while (it.hasNext()) {
                String li = it.next();
                if (li.startsWith("/")) li = li.substring(1);
                if (li.endsWith("*")) {
                    trustedTemplatePrefixes.add(li.substring(0, li.length() - 1));
                } else {
                    trustedTemplateNames.add(li);
                }
            }
        } else {
            trustedTemplateNames = Collections.EMPTY_SET;
            trustedTemplatePrefixes = Collections.EMPTY_LIST;
        }
    }

    @Override
    public Class resolve(String className, Environment env, Template template)
    throws TemplateException {
        String templateName = safeGetTemplateName(template);
        
        if (templateName != null
                && (trustedTemplateNames.contains(templateName)
                        || hasMatchingPrefix(templateName))) {
            return TemplateClassResolver.UNRESTRICTED.resolve(className, env, template);
        } else {
            if (!allowedClasses.contains(className)) {
                throw new TemplateException(env,
                        "Instantiating ", className, " is not allowed in the template for security reasons. (If you "
                        + "run into this problem when using ?new in a template, you may want to check the \"",
                        MutableProcessingConfiguration.NEW_BUILTIN_CLASS_RESOLVER_KEY,
                        "\" setting in the FreeMarker configuration.)");
            } else {
                try {
                    return _ClassUtils.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new TemplateException(e, env);
                }
            }
        }
    }

    /**
     * Extract the template name from the template object which will be matched
     * against the trusted template names and pattern. 
     */
    protected String safeGetTemplateName(Template template) {
        if (template == null) return null;
        
        String name = template.getLookupName();
        if (name == null) return null;

        // Detect exploits, return null if one is suspected:
        String decodedName = name;
        if (decodedName.indexOf('%') != -1) {
            decodedName = _StringUtils.replace(decodedName, "%2e", ".", false, false);
            decodedName = _StringUtils.replace(decodedName, "%2E", ".", false, false);
            decodedName = _StringUtils.replace(decodedName, "%2f", "/", false, false);
            decodedName = _StringUtils.replace(decodedName, "%2F", "/", false, false);
            decodedName = _StringUtils.replace(decodedName, "%5c", "\\", false, false);
            decodedName = _StringUtils.replace(decodedName, "%5C", "\\", false, false);
        }
        int dotDotIdx = decodedName.indexOf("..");
        if (dotDotIdx != -1) {
            int before = dotDotIdx - 1 >= 0 ? decodedName.charAt(dotDotIdx - 1) : -1;
            int after = dotDotIdx + 2 < decodedName.length() ? decodedName.charAt(dotDotIdx + 2) : -1;
            if ((before == -1 || before == '/' || before == '\\')
                    && (after == -1 || after == '/' || after == '\\')) {
                return null;
            }
        }
        
        return name.startsWith("/") ? name.substring(1) : name;
    }

    private boolean hasMatchingPrefix(String name) {
        for (int i = 0; i < trustedTemplatePrefixes.size(); i++) {
            String prefix = (String) trustedTemplatePrefixes.get(i);
            if (name.startsWith(prefix)) return true;
        }
        return false;
    }

}
