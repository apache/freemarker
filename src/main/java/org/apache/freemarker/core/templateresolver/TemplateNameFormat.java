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

package org.apache.freemarker.core.templateresolver;

/**
 * Symbolized template name format. The API of this class isn't exposed as it's too immature, so custom
 * template name formats aren't possible yet.
 *
 * @since 2.3.22
 */
public abstract class TemplateNameFormat {

    protected TemplateNameFormat() {
       //  
    }
    
    /**
     * Implements {@link TemplateResolver#toRootBasedName(String, String)}; see more there.
     */
    public abstract String toRootBasedName(String baseName, String targetName) throws MalformedTemplateNameException;
    
    /**
     * Implements {@link TemplateResolver#normalizeRootBasedName(String)}; see more there.
     */
    public abstract String normalizeRootBasedName(String name) throws MalformedTemplateNameException;

    protected void checkNameHasNoNullCharacter(final String name) throws MalformedTemplateNameException {
        if (name.indexOf(0) != -1) {
            throw new MalformedTemplateNameException(name,
                    "Null character (\\u0000) in the name; possible attack attempt");
        }
    }
    
    protected MalformedTemplateNameException newRootLeavingException(final String name) {
        return new MalformedTemplateNameException(name, "Backing out from the root directory is not allowed");
    }
    
}
