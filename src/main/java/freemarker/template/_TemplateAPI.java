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

package freemarker.template;

import freemarker.core.TemplateObject;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _TemplateAPI {
    
    public static final Version VERSION_2_3_0 = new Version(2, 3, 0);  
    public static final Version VERSION_2_3_19 = new Version(2, 3, 19);  
    public static final Version VERSION_2_3_20 = new Version(2, 3, 20);  
    public static final Version VERSION_2_3_21 = new Version(2, 3, 21);  
    public static final int VERSION_INT_2_3_0 = VERSION_2_3_0.intValue();
    public static final int VERSION_INT_2_3_19 = VERSION_2_3_19.intValue();
    public static final int VERSION_INT_2_3_20 = VERSION_2_3_20.intValue();
    public static final int VERSION_INT_2_3_21 = VERSION_2_3_21.intValue();
    
    public static void checkVersionSupported(Version incompatibleImprovements) {
        if (incompatibleImprovements != null) {
            int iciV = incompatibleImprovements.intValue();
            if (iciV > Configuration.getVersion().intValue()) {
                throw new IllegalArgumentException("The FreeMarker version requested by \"incompatibleImprovements\" was "
                        + incompatibleImprovements + ", but the installed FreeMarker version is only "
                        + Configuration.getVersion() + ". You may need to upgrade FreeMarker in your project.");
            }
            if (iciV < 200300) {
                throw new IllegalArgumentException("\"incompatibleImprovements\" must be at least 2.3.0.");
            }
        }
    }
    
    public static int getTemplateLanguageVersionAsInt(TemplateObject to) {
        return to.getTemplate().getTemplateLanguageVersion().intValue();
    }
    
    /** For unit testing only */
    public static void DefaultObjectWrapperFactory_clearInstanceCache() {
        DefaultObjectWrapperBuilder.clearInstanceCache();
    }
    
}
