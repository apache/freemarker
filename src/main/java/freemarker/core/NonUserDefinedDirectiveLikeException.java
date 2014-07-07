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

import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateTransformModel;

/**
 * Indicates that a {@link TemplateDirectiveModel} or {@link TemplateTransformModel} or {@link Macro} value was
 * expected, but the value had a different type.
 * 
 * @since 2.3.21
 */
class NonUserDefinedDirectiveLikeException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] {
        TemplateDirectiveModel.class, TemplateTransformModel.class, Macro.class };
    
    public NonUserDefinedDirectiveLikeException(Environment env) {
        super(env, "Expecting user-defined directive, transform or macro value here");
    }

    public NonUserDefinedDirectiveLikeException(String description, Environment env) {
        super(env, description);
    }

    NonUserDefinedDirectiveLikeException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonUserDefinedDirectiveLikeException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "user-defined directive, transform or macro", EXPECTED_TYPES, env);
    }

    NonUserDefinedDirectiveLikeException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "user-defined directive, transform or macro", EXPECTED_TYPES, tip, env);
    }

    NonUserDefinedDirectiveLikeException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "user-defined directive, transform or macro", EXPECTED_TYPES, tips, env);
    }    

}
