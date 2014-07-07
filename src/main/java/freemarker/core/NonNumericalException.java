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
import freemarker.template.TemplateNumberModel;

/**
 * Indicates that a {@link TemplateNumberModel} value was expected, but the value had a different type.
 */
public class NonNumericalException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateNumberModel.class };
    
    public NonNumericalException(Environment env) {
        super(env, "Expecting numerical value here");
    }

    public NonNumericalException(String description, Environment env) {
        super(env, description);
    }
 
    NonNumericalException(_ErrorDescriptionBuilder description, Environment env) {
        super(env, description);
    }

    NonNumericalException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "number", EXPECTED_TYPES, env);
    }

    NonNumericalException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "number", EXPECTED_TYPES, tip, env);
    }

    NonNumericalException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "number", EXPECTED_TYPES, tips, env);
    }

    
    static NonNumericalException newMalformedNumberException(Expression blamed, String text, Environment env) {
        return new NonNumericalException(
                new _ErrorDescriptionBuilder(new Object[] {
                        "Can't convert this string to number: ", new _DelayedJQuote(text) })
                .blame(blamed),
                env);
    }
    
}
