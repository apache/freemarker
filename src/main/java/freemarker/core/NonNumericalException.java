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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * A {@link TemplateException} that 
 * indicates that the internals expected an expression
 * to evaluate to a numerical value and it didn't.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */
public class NonNumericalException extends UnexpectedTypeException {

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
        super(blamed, model, "number", env);
    }

    NonNumericalException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "number", tip, env);
    }

    NonNumericalException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "number", tips, env);
    }

    
    static NonNumericalException newMalformedNumberException(Expression blamed, String text, Environment env) {
        return new NonNumericalException(
                new _ErrorDescriptionBuilder(new Object[] {
                        "Can't convert this string to number: ", new _DelayedJQuote(text) })
                .blame(blamed),
                env);
    }
    
}
