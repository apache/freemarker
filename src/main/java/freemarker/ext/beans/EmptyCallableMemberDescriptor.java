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

/**
 * Represents that no member was chosen. Why it wasn't is represented by the two singleton instances,
 * {@link #NO_SUCH_METHOD} and {@link #AMBIGUOUS_METHOD}. (Note that instances of these are cached associated with the
 * argument types, thus it shouldn't store details that are specific to the actual argument values. In fact, it better
 * remains a set of singletons.)     
 */
final class EmptyCallableMemberDescriptor extends MaybeEmptyCallableMemberDescriptor {
    
    static final EmptyCallableMemberDescriptor NO_SUCH_METHOD = new EmptyCallableMemberDescriptor();
    static final EmptyCallableMemberDescriptor AMBIGUOUS_METHOD = new EmptyCallableMemberDescriptor();
    
    private EmptyCallableMemberDescriptor() { };
    
}