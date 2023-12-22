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

package freemarker.core;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * Indicates that a {@link TemplateScalarModel} (or maybe something that can be automatically coerced
 * to that) or {@link TemplateMarkupOutputModel} value was expected, but the value had a different type.
 */
public class NonStringOrTemplateOutputException extends UnexpectedTypeException {

    static final String STRING_COERCABLE_TYPES_OR_TOM_DESC
            = NonStringException.STRING_COERCABLE_TYPES_DESC + ", or \"template output\" ";
    
    static final Class[] STRING_COERCABLE_TYPES_AND_TOM;
    static {
        STRING_COERCABLE_TYPES_AND_TOM = new Class[NonStringException.STRING_COERCABLE_TYPES.length + 1];
        int i;
        for (i = 0; i < NonStringException.STRING_COERCABLE_TYPES.length; i++) {
            STRING_COERCABLE_TYPES_AND_TOM[i] = NonStringException.STRING_COERCABLE_TYPES[i];
        }
        STRING_COERCABLE_TYPES_AND_TOM[i] = TemplateMarkupOutputModel.class;
    };
    
    private static final String DEFAULT_DESCRIPTION
            = "Expecting " + NonStringOrTemplateOutputException.STRING_COERCABLE_TYPES_OR_TOM_DESC + " value here";

    public NonStringOrTemplateOutputException(Environment env) {
        super(env, DEFAULT_DESCRIPTION);
    }

    public NonStringOrTemplateOutputException(String description, Environment env) {
        super(env, description);
    }
 
    NonStringOrTemplateOutputException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonStringOrTemplateOutputException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, NonStringOrTemplateOutputException.STRING_COERCABLE_TYPES_OR_TOM_DESC, STRING_COERCABLE_TYPES_AND_TOM, env);
    }

    NonStringOrTemplateOutputException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, NonStringOrTemplateOutputException.STRING_COERCABLE_TYPES_OR_TOM_DESC, STRING_COERCABLE_TYPES_AND_TOM, tip, env);
    }

    NonStringOrTemplateOutputException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, NonStringOrTemplateOutputException.STRING_COERCABLE_TYPES_OR_TOM_DESC, STRING_COERCABLE_TYPES_AND_TOM, tips, env);
    }
        
}
