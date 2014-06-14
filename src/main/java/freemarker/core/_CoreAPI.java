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

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

import freemarker.template.ObjectWrapper;
import freemarker.template.Version;


/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CoreAPI {
    
    // Can't be instantiated
    private _CoreAPI() { }

    public static final String STACK_SECTION_SEPARATOR = Environment.STACK_SECTION_SEPARATOR;
    
    public static final int DEFAULT_TL_AND_OW_CHANGE_VERSION = Configurable.DEFAULT_TL_AND_OW_CHANGE_VERSION;
    
    /**
     * Returns the names of the currently supported "built-ins" ({@code expr?builtin_name}-like things).
     * @return {@link Set} of {@link String}-s. 
     */
    public static Set/*<String>*/ getSupportedBuiltInNames() {
        return Collections.unmodifiableSet(BuiltIn.builtins.keySet());
    }
    
    public static String instructionStackItemToString(TemplateElement stackEl) {
        return Environment.instructionStackItemToString(stackEl);
    }
    
    public static TemplateElement[] getInstructionStackSnapshot(Environment env) {
        return env.getInstructionStackSnapshot();
    }
    
    public static void outputInstructionStack(
            TemplateElement[] instructionStackSnapshot, PrintWriter pw) {
        Environment.outputInstructionStack(instructionStackSnapshot, pw);
    }
    
    public static ObjectWrapper getDefaultObjectWrapper(Version incompatibleImprovements) {
        return Configurable.getDefaultObjectWrapper(incompatibleImprovements);
    }
    
}
