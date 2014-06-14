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

import java.beans.MethodDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

class AlphabeticalMethodSorter implements MethodSorter {

    private final boolean desc;
    
    public AlphabeticalMethodSorter(boolean desc) {
        this.desc = desc;
    }

    public MethodDescriptor[] sortMethodDescriptors(MethodDescriptor[] methodDescriptors) {
        ArrayList<MethodDescriptor> ls = new ArrayList<MethodDescriptor>(Arrays.asList(methodDescriptors));
        Collections.sort(ls, new Comparator<MethodDescriptor>() {
            public int compare(MethodDescriptor o1, MethodDescriptor o2) {
                int res = o1.getMethod().toString().compareTo(o2.getMethod().toString());
                return desc ? -res : res;
            }
        });
        return ls.toArray(new MethodDescriptor[ls.size()]);
    }
    
}