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

package freemarker.ext.beans;


import freemarker.template.Version;

/**
 * Used so that the order in which the methods are added to the introspection cache is deterministic. 
 */
public abstract class BeansWrapperWithShortedMethods extends BeansWrapper {
    
    public BeansWrapperWithShortedMethods(boolean desc) {
        this.setMethodSorter(new AlphabeticalMethodSorter(desc));
    }

    public BeansWrapperWithShortedMethods(Version incompatibleImprovements, boolean desc) {
        super(incompatibleImprovements);
        this.setMethodSorter(new AlphabeticalMethodSorter(desc));
    }

}
