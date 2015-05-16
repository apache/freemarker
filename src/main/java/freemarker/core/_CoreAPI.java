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

import java.io.Writer;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;


/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CoreAPI {
    
    public static final String ERROR_MESSAGE_HR = "----";

    // Can't be instantiated
    private _CoreAPI() { }
    
    public static final Set/*<String>*/ BUILT_IN_DIRECTIVE_NAMES;
    static {
        Set/*<String>*/ names = new TreeSet();
        names.add("assign");
        names.add("attempt");
        names.add("break");
        names.add("call");
        names.add("case");
        names.add("comment");
        names.add("compress");
        names.add("default");
        names.add("else");
        names.add("elseif");
        names.add("elseIf");
        names.add("escape");
        names.add("fallback");
        names.add("flush");
        names.add("foreach");
        names.add("forEach");
        names.add("ftl");
        names.add("function");
        names.add("global");
        names.add("if");
        names.add("import");
        names.add("include");
        names.add("items");
        names.add("list");
        names.add("local");
        names.add("lt");
        names.add("macro");
        names.add("nested");
        names.add("noescape");
        names.add("noEscape");
        names.add("noparse");
        names.add("noParse");
        names.add("nt");
        names.add("recover");
        names.add("recurse");
        names.add("return");
        names.add("rt");
        names.add("sep");
        names.add("setting");
        names.add("stop");
        names.add("switch");
        names.add("t");
        names.add("transform");
        names.add("visit");
        BUILT_IN_DIRECTIVE_NAMES = Collections.unmodifiableSet(names);
    }
    
    /**
     * Returns the names of the currently supported "built-ins" ({@code expr?builtin_name}-like things).
     * @return {@link Set} of {@link String}-s. 
     */
    public static Set/*<String>*/ getSupportedBuiltInNames() {
        return Collections.unmodifiableSet(BuiltIn.builtins.keySet());
    }
    
    public static void appendInstructionStackItem(TemplateElement stackEl, StringBuffer sb) {
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
     * [FM 2.4] getSettingNames() becomes to public; remove this.
     */
    public static Set/*<String>*/ getConfigurableSettingNames(Configurable cfgable, boolean camelCase) {
        return cfgable.getSettingNames(camelCase);
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
    
}
