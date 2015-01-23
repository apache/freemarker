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

import freemarker.template.Configuration;

final class TemplateLoaderUtils {

    private TemplateLoaderUtils() {
        // Not meant to be instantiated
    }

    public static String getClassNameForToString(TemplateLoader templateLoader) {
        final Class tlClass = templateLoader.getClass();
        final Package tlPackage = tlClass.getPackage();
        return tlPackage == Configuration.class.getPackage() || tlPackage == TemplateLoader.class.getPackage()
                ? getSimpleName(tlClass) : tlClass.getName();
    }

    // [Java 5] Replace with Class.getSimpleName()
    private static String getSimpleName(final Class tlClass) {
        final String name = tlClass.getName();
        int lastDotIdx = name.lastIndexOf('.'); 
        return lastDotIdx < 0 ? name : name.substring(lastDotIdx + 1);
    }
    
}
