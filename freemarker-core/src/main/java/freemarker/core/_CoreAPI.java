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

import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.ClassUtil;


/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */
public class _CoreAPI {
    // ATTENTION! Don't refer to other classes in the static initializer of this class! Fields that need that must be
    // moved into separate class, to avoid class init deadlocks.

    public static final String ERROR_MESSAGE_HR = "----";

    // Can't be instantiated
    private _CoreAPI() { }

    private static void addName(Set<String> allNames, Set<String> lcNames, Set<String> ccNames,
            String commonName) {
        allNames.add(commonName);
        lcNames.add(commonName);
        ccNames.add(commonName);
    }
    
    private static void addName(Set<String> allNames, Set<String> lcNames, Set<String> ccNames,
            String lcName, String ccName) {
        allNames.add(lcName);
        allNames.add(ccName);
        lcNames.add(lcName);
        ccNames.add(ccName);
    }
    
    public static final Set<String> ALL_BUILT_IN_DIRECTIVE_NAMES;
    public static final Set<String> LEGACY_BUILT_IN_DIRECTIVE_NAMES;
    public static final Set<String> CAMEL_CASE_BUILT_IN_DIRECTIVE_NAMES;
    static {
        Set<String> allNames = new TreeSet();
        Set<String> lcNames = new TreeSet();
        Set<String> ccNames = new TreeSet();
        
        addName(allNames, lcNames, ccNames, "assign");
        addName(allNames, lcNames, ccNames, "attempt");
        addName(allNames, lcNames, ccNames, "autoesc", "autoEsc");
        addName(allNames, lcNames, ccNames, "break");
        addName(allNames, lcNames, ccNames, "call");
        addName(allNames, lcNames, ccNames, "case");
        addName(allNames, lcNames, ccNames, "comment");
        addName(allNames, lcNames, ccNames, "compress");
        addName(allNames, lcNames, ccNames, "continue");
        addName(allNames, lcNames, ccNames, "default");
        addName(allNames, lcNames, ccNames, "else");
        addName(allNames, lcNames, ccNames, "elseif", "elseIf");
        addName(allNames, lcNames, ccNames, "escape");
        addName(allNames, lcNames, ccNames, "fallback");
        addName(allNames, lcNames, ccNames, "flush");
        addName(allNames, lcNames, ccNames, "foreach", "forEach");
        addName(allNames, lcNames, ccNames, "ftl");
        addName(allNames, lcNames, ccNames, "function");
        addName(allNames, lcNames, ccNames, "global");
        addName(allNames, lcNames, ccNames, "if");
        addName(allNames, lcNames, ccNames, "import");
        addName(allNames, lcNames, ccNames, "include");
        addName(allNames, lcNames, ccNames, "items");
        addName(allNames, lcNames, ccNames, "list");
        addName(allNames, lcNames, ccNames, "local");
        addName(allNames, lcNames, ccNames, "lt");
        addName(allNames, lcNames, ccNames, "macro");
        addName(allNames, lcNames, ccNames, "nested");
        addName(allNames, lcNames, ccNames, "noautoesc", "noAutoEsc");
        addName(allNames, lcNames, ccNames, "noescape", "noEscape");
        addName(allNames, lcNames, ccNames, "noparse", "noParse");
        addName(allNames, lcNames, ccNames, "nt");
        addName(allNames, lcNames, ccNames, "outputformat", "outputFormat");
        addName(allNames, lcNames, ccNames, "recover");
        addName(allNames, lcNames, ccNames, "recurse");
        addName(allNames, lcNames, ccNames, "return");
        addName(allNames, lcNames, ccNames, "rt");
        addName(allNames, lcNames, ccNames, "sep");
        addName(allNames, lcNames, ccNames, "setting");
        addName(allNames, lcNames, ccNames, "stop");
        addName(allNames, lcNames, ccNames, "switch");
        addName(allNames, lcNames, ccNames, "t");
        addName(allNames, lcNames, ccNames, "transform");
        addName(allNames, lcNames, ccNames, "visit");
        
        ALL_BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(allNames);
        LEGACY_BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(lcNames);
        CAMEL_CASE_BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(ccNames);
    }
    
    /**
     * Returns the names of the currently supported "built-ins" ({@code expr?builtin_name}-like things).
     * 
     * @param namingConvention
     *            One of {@link Configuration#AUTO_DETECT_NAMING_CONVENTION},
     *            {@link Configuration#LEGACY_NAMING_CONVENTION}, and
     *            {@link Configuration#CAMEL_CASE_NAMING_CONVENTION}. If it's
     *            {@link Configuration#AUTO_DETECT_NAMING_CONVENTION} then the union of the names in all the naming
     *            conventions is returned.
     */
    public static Set<String> getSupportedBuiltInNames(int namingConvention) {
        Set<String> names;
        if (namingConvention == Configuration.AUTO_DETECT_NAMING_CONVENTION) {
            names = BuiltIn.BUILT_INS_BY_NAME.keySet();
        } else if (namingConvention == Configuration.LEGACY_NAMING_CONVENTION) {
            names = BuiltIn.SNAKE_CASE_NAMES;
        } else if (namingConvention == Configuration.CAMEL_CASE_NAMING_CONVENTION) {
            names = BuiltIn.CAMEL_CASE_NAMES;
        } else {
            throw new IllegalArgumentException("Unsupported naming convention constant: " + namingConvention);
        }
        return Collections.unmodifiableSet(names);
    }
    
    public static void appendInstructionStackItem(TemplateElement stackEl, StringBuilder sb) {
        Environment.appendInstructionStackItem(stackEl, sb);
    }
    
    public static TemplateElement[] getInstructionStackSnapshot(Environment env) {
        return env.getInstructionStackSnapshot();
    }
    
    public static void outputInstructionStack(
            TemplateElement[] instructionStackSnapshot, boolean terseMode, Writer pw) {
        Environment.outputInstructionStack(instructionStackSnapshot, terseMode, pw);
    }

    /**
     * ATTENTION: This is used by https://github.com/kenshoo/freemarker-online. Don't break backward
     * compatibility without updating that project too! 
     */
    static final public void addThreadInterruptedChecks(Template template) {
        try {
            new ThreadInterruptionSupportTemplatePostProcessor().postProcess(template);
        } catch (TemplatePostProcessorException e) {
            throw new RuntimeException("Template post-processing failed", e);
        }
    }
    
    static final public void checkHasNoNestedContent(TemplateDirectiveBody body)
            throws NestedContentNotSupportedException {
        NestedContentNotSupportedException.check(body);
    }
    
    static final public void replaceText(TextBlock textBlock, String text) {
        textBlock.replaceText(text);
    }

    /**
     * @throws IllegalArgumentException
     *             if the type of the some of the values isn't as expected
     */
    public static void checkSettingValueItemsType(String somethingsSentenceStart, Class<?> expectedClass,
            Collection<?> values) {
        if (values == null) return;
        for (Object value : values) {
            if (!expectedClass.isInstance(value)) {
                throw new IllegalArgumentException(somethingsSentenceStart + " must be instances of "
                        + ClassUtil.getShortClassName(expectedClass) + ", but one of them was a(n) "
                        + ClassUtil.getShortClassNameOfObject(value) + ".");
            }
        }
    }
    
    /**
     * The work around the problematic cases where we should throw a {@link TemplateException}, but we are inside
     * a {@link TemplateModel} method and so we can only throw {@link TemplateModelException}-s.  
     */
    public static TemplateModelException ensureIsTemplateModelException(String modelOpMsg, TemplateException e) {
        if (e instanceof TemplateModelException) {
            return (TemplateModelException) e;
        } else {
            return new _TemplateModelException(
                    _TemplateAPI.getBlamedExpression(e), e.getCause(), e.getEnvironment(), modelOpMsg);
        }
    }
    
    public static TemplateElement getParentElement(TemplateElement te) {
        return te.getParentElement();
    }

    public static TemplateElement getChildElement(TemplateElement te, int index) {
        return te.getChild(index);
    }

    public static void setPreventStrippings(FMParser parser, boolean preventStrippings) {
        parser.setPreventStrippings(preventStrippings);
    }

    public static boolean isLazilyGeneratedSequenceModel(TemplateCollectionModel model) {
        return model instanceof LazilyGeneratedCollectionModel && ((LazilyGeneratedCollectionModel) model).isSequence();
    }
}
