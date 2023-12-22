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

/**
 * Indicates that a {@link TemplateMarkupOutputModel} value was expected, but the value had a different type.
 * 
 * @since 2.3.24
 */
public class NonMarkupOutputException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] { TemplateMarkupOutputModel.class };
    
    public NonMarkupOutputException(Environment env) {
        super(env, "Expecting markup output value here");
    }

    public NonMarkupOutputException(String description, Environment env) {
        super(env, description);
    }

    NonMarkupOutputException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonMarkupOutputException(
            Expression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "markup output", EXPECTED_TYPES, env);
    }

    NonMarkupOutputException(
            Expression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(blamed, model, "markup output", EXPECTED_TYPES, tip, env);
    }

    NonMarkupOutputException(
            Expression blamed, TemplateModel model, String[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "markup output", EXPECTED_TYPES, tips, env);
    }    

}
