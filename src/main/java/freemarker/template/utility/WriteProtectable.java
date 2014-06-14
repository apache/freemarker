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

package freemarker.template.utility;

/**
 * Implemented by objects that can be made <em>permanently</em> read-only. This usually meant to freeze the
 * configuration JavaBean properties, so that the object can be safely shared among independently developed components.
 * 
 * @since 2.3.21
 */
public interface WriteProtectable {

    /**
     * Makes this object permanently read-only.
     */
    void writeProtect();
    
    boolean isWriteProtected();
    
}
