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

package freemarker.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import freemarker.cache.TemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.ClassUtil;

/**
 * A {@link TemplateClassResolver} that resolves only the classes whose name was specified in the constructor.
 */
public class OptInTemplateClassResolver implements TemplateClassResolver {
    private final Set<String> allowedClasses;
    private final List<String> trustedTemplatePrefixes;
    private final Set<String> trustedTemplateNames;

    /**
     * Creates a new instance.
     *
     * @param allowedClasses
     *         the {@link Set} of {@link String}-s that contains the full-qualified names of the allowed classes. Can
     *         be
     *         <code>null</code> (means not class is allowed).
     * @param trustedTemplates
     *         the {@link List} of {@link String}-s that contains template names (i.e., template root directory relative
     *         paths) and prefix patterns (like <code>"include/*"</code>) of templates for which
     *         {@link TemplateClassResolver#SAFER_RESOLVER} will be used (which is not as safe as
     *         {@link OptInTemplateClassResolver}). The list items need not start with <code>"/"</code> (if they are, it
     *         will be removed). List items ending with <code>"*"</code> are treated as prefixes (i.e.
     *         <code>"foo*"</code> matches <code>"foobar"</code>,
     *         <code>"foo/bar/baaz"</code>, <code>"foowhatever/bar/baaz"</code>,
     *         etc.). The <code>"*"</code> has no special meaning anywhere else. The matched template name is the name
     *         (template root directory relative path) of the template that directly (lexically) contains the operation
     *         (like <code>?new</code>) that wants to get the class. Thus, if a trusted template includes a non-trusted
     *         template, the
     *         <code>allowedClasses</code> restriction will apply in the included
     *         template. This parameter can be <code>null</code> (means no trusted templates).
     */
    @SuppressWarnings("unchecked") // Backward compatibility...
    public OptInTemplateClassResolver(
            Set allowedClasses, List trustedTemplates) {
        this.allowedClasses = allowedClasses != null ? allowedClasses : Collections.EMPTY_SET;
        if (trustedTemplates != null) {
            trustedTemplateNames = new HashSet<>();
            trustedTemplatePrefixes = new ArrayList<>();

            for (Object trustedTemplate : trustedTemplates) {
                String li = (String) trustedTemplate;
                if (li.startsWith("/")) li = li.substring(1);
                if (li.endsWith("*")) {
                    trustedTemplatePrefixes.add(li.substring(0, li.length() - 1));
                } else {
                    trustedTemplateNames.add(li);
                }
            }
        } else {
            trustedTemplateNames = Collections.emptySet();
            trustedTemplatePrefixes = Collections.emptyList();
        }
    }

    @Override
    public Class resolve(String className, Environment env, Template template)
            throws TemplateException {
        String templateName = safeGetTemplateName(template);

        if (templateName != null
                && (trustedTemplateNames.contains(templateName)
                || hasMatchingPrefix(templateName))) {
            return TemplateClassResolver.SAFER_RESOLVER.resolve(className, env, template);
        } else {
            if (!allowedClasses.contains(className)) {
                throw new _MiscTemplateException(env,
                        "Instantiating ", className, " is not allowed in the template for security reasons. (If you "
                        + "run into this problem when using ?new in a template, you may want to check the \"",
                        Configurable.NEW_BUILTIN_CLASS_RESOLVER_KEY,
                        "\" setting in the FreeMarker configuration.)");
            } else {
                try {
                    return ClassUtil.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new _MiscTemplateException(e, env);
                }
            }
        }
    }

    /**
     * Extract the template name from the template object which will be matched against the trusted template names and
     * pattern.
     */
    protected String safeGetTemplateName(Template template) {
        if (template == null) return null;

        String name = template.getName();
        if (name == null || mayContainsBackStep(name)) {
            return null;
        }

        return name.startsWith("/") ? name.substring(1) : name;
    }

    private static final int DOT_NAME_START_TOKEN = -1;
    private static final int DOT_DOT_NAME_START_TOKEN = -2;

    /**
     * Checks if a path contains a name that's {@code ".."} (stepping higher in the hierarchy), but it assumes that any
     * %xx URL escaping will be resolved (though a good behaving {@link TemplateLoader} shouldn't do that), and that
     * backslash will be interpreted as {@code \\}. For example, for {@code "../x"} the result is {@code true}, while
     * for {@code "..x"} it's {@code false} (as {@code "..x"} has no special meaning). It's assumed that the bounds of
     * the input stings are also the bounds of the path, so {@code foo/..} and {@code ../foo} are both considered to
     * contain {@code ".."}. And so does {@code ".."} in itself.
     *
     * <p>Note that this algorithm errs on the side of caution. It's unlikely that legitimate template names trigger
     * it though.
     *
     * <p>Note that we also block the character with code point 0, as some native methods might interpret it as
     * end-of-string. However, we don't block {@code %00} as that would a bit too backward-incompatible, and also
     * exploiting that requires two weakly implemented components in a row, so it's less likely exploitable.
     */
    static boolean mayContainsBackStep(String path) {
        int len = path.length();
        int pos = 0;

        int lastToken = '/';
        while (pos < len) {
            char c = path.charAt(pos++);

            if (c == '%' && pos + 1 < len) {
                char hexDigit1 = path.charAt(pos);
                if (hexDigit1 == '2' || hexDigit1 == '5') {
                    char hexDigit2 = path.charAt(pos + 1);
                    if (hexDigit1 == '2') {
                        if (hexDigit2 == 'e' || hexDigit2 == 'E') {
                            c = '.';
                            pos += 2;
                        } else if (hexDigit2 == 'f' || hexDigit2 == 'F') {
                            c = '/';
                            pos += 2;
                        }
                    } else if (hexDigit2 == 'c' || hexDigit2 == 'C') {
                        c = '/'; // %5C is '\', but we normalize it to '/'
                        pos += 2;
                    }
                }
            } else if (c == '\\') {
                c = '/';
            } else if (c == 0) {
                return true;
            }

            // Now c is the logical character: relevant %xx-s are decoded, and \ is normalized to /

            if (c == '.') {
                if (lastToken == '/') {
                    lastToken = DOT_NAME_START_TOKEN;
                } else if (lastToken == DOT_NAME_START_TOKEN) {
                    lastToken = DOT_DOT_NAME_START_TOKEN;
                } else {
                    lastToken = c;
                }
            } else if (c == '/' && lastToken == DOT_DOT_NAME_START_TOKEN) {
                return true;
            } else {
                lastToken = c;
            }
        }
        return lastToken == DOT_DOT_NAME_START_TOKEN;
    }

    private boolean hasMatchingPrefix(String name) {
        for (String trustedTemplatePrefix : trustedTemplatePrefixes) {
            if (name.startsWith(trustedTemplatePrefix)) return true;
        }
        return false;
    }
}
