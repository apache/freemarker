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

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;

/**
 * Indicates that a {@link TemplateDateModel} value was expected, but the value had a different type.
 */
public class NonDateException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateDateModel.class };

    public NonDateException(Environment env) {
        super(env, "Expecting date/time value here");
    }

    public NonDateException(String description, Environment env) {
        super(env, description);
    }

    NonDateException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "date/time", EXPECTED_TYPES, env);
    }

    NonDateException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "date/time", EXPECTED_TYPES, tip, env);
    }

    NonDateException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "date/time", EXPECTED_TYPES, tips, env);
    }    
        
}
