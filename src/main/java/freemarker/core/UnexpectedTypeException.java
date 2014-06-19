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
 * The type of a value differs from what was expected.
 * 
 * @since 2.3.20
 */
public class UnexpectedTypeException extends TemplateException {

    public UnexpectedTypeException(Environment env, String description) {
        super(description, env);
    }

    UnexpectedTypeException(Environment env, _ErrorDescriptionBuilder description) {
        super(null, env, description, true);
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedType, Environment env)
            throws InvalidReferenceException {
        super(null, env, newDesciptionBuilder(blamed, model, expectedType, env), true);
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedType, String tip, Environment env)
            throws InvalidReferenceException {
        super(null, env, newDesciptionBuilder(blamed, model, expectedType, env).tip(tip), true);
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedType, String[] tips, Environment env)
            throws InvalidReferenceException {
        super(null, env, newDesciptionBuilder(blamed, model, expectedType, env).tips(tips), true);
    }
    
    private static _ErrorDescriptionBuilder newDesciptionBuilder(
            Expression blamed, TemplateModel model, String expectedType, Environment env)
            throws InvalidReferenceException {
        if (model == null) throw InvalidReferenceException.getInstance(blamed, env);
        return new _ErrorDescriptionBuilder(
                unexpectedTypeErrorDescription(expectedType, model))
                .blame(blamed).showBlamer(true);
    }

    private static Object[] unexpectedTypeErrorDescription(String expectedType, TemplateModel model) {
        return new Object[] {
                "Expected ", new _DelayedAOrAn(expectedType), ", but this evaluated to ",
                new _DelayedAOrAn(new _DelayedFTLTypeDescription(model)), ":"};
    }
    
}
