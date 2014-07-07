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

/**
 * Indicates that a {@link Environment.Namespace} value was expected, but the value had a different type.
 * 
 * @since 2.3.21
 */
class NonNamespaceException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { Environment.Namespace.class };
    
    public NonNamespaceException(Environment env) {
        super(env, "Expecting namespace value here");
    }

    public NonNamespaceException(String description, Environment env) {
        super(env, description);
    }

    NonNamespaceException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonNamespaceException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "namespace", EXPECTED_TYPES, env);
    }

    NonNamespaceException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "namespace", EXPECTED_TYPES, tip, env);
    }

    NonNamespaceException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "namespace", EXPECTED_TYPES, tips, env);
    }    

}
