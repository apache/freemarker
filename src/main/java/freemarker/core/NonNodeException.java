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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateNodeModel;

/**
 * Indicates that a {@link TemplateNodeModel} value was expected, but the value had a different type.
 * 
 * @since 2.3.21
 */
public class NonNodeException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateNodeModel.class };
    
    public NonNodeException(Environment env) {
        super(env, "Expecting node value here");
    }

    public NonNodeException(String description, Environment env) {
        super(env, description);
    }

    NonNodeException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonNodeException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "node", EXPECTED_TYPES, env);
    }

    NonNodeException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "node", EXPECTED_TYPES, tip, env);
    }

    NonNodeException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "node", EXPECTED_TYPES, tips, env);
    }    

}
