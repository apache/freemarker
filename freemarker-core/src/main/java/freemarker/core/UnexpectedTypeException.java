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

import java.util.Arrays;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;

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
        super(null, env, null, description);
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedTypesDesc, Class[] expectedTypes, Environment env)
            throws InvalidReferenceException {
        super(null, env, blamed, newDescriptionBuilder(blamed, null, model, expectedTypesDesc, expectedTypes, env));
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedTypesDesc, Class[] expectedTypes, String tip,
            Environment env)
            throws InvalidReferenceException {
        super(null, env, blamed, newDescriptionBuilder(blamed, null, model, expectedTypesDesc, expectedTypes, env)
                .tip(tip));
    }

    UnexpectedTypeException(
            Expression blamed, TemplateModel model, String expectedTypesDesc, Class[] expectedTypes, Object[] tips,
            Environment env)
            throws InvalidReferenceException {
        super(null, env, blamed, newDescriptionBuilder(blamed, null, model, expectedTypesDesc, expectedTypes, env)
                .tips(tips));
    }

    UnexpectedTypeException(
            String blamedAssignmentTargetVarName, TemplateModel model, String expectedTypesDesc, Class[] expectedTypes,
            Object[] tips,
            Environment env)
            throws InvalidReferenceException {
        super(null, env, null, newDescriptionBuilder(
                null, blamedAssignmentTargetVarName, model, expectedTypesDesc, expectedTypes, env).tips(tips));
    }
    
    /**
     * @param blamedAssignmentTargetVarName
     *            Used for assignments that use {@code +=} and such, in which case the {@code blamed} expression
     *            parameter will be null {@code null} and this parameter will be non-{null}.
     */
    private static _ErrorDescriptionBuilder newDescriptionBuilder(
            Expression blamed, String blamedAssignmentTargetVarName,
            TemplateModel model, String expectedTypesDesc, Class[] expectedTypes, Environment env)
            throws InvalidReferenceException {
        if (model == null) throw InvalidReferenceException.getInstance(blamed, env);

        _ErrorDescriptionBuilder errorDescBuilder = new _ErrorDescriptionBuilder(
                unexpectedTypeErrorDescription(expectedTypesDesc, blamed, blamedAssignmentTargetVarName, model))
                .blame(blamed).showBlamer(true);
        if (model instanceof _UnexpectedTypeErrorExplainerTemplateModel) {
            Object[] tip = ((_UnexpectedTypeErrorExplainerTemplateModel) model).explainTypeError(expectedTypes);
            if (tip != null) {
                errorDescBuilder.tip(tip);
            }
        }
        if (model instanceof TemplateCollectionModel
                && (Arrays.asList(expectedTypes).contains(TemplateSequenceModel.class)
                        || Arrays.asList(expectedTypes).contains(TemplateCollectionModelEx.class))) {
            errorDescBuilder.tip("As the problematic value contains a collection of items, you could convert it "
                    + "to a sequence like someValue?sequence. Be sure though that you won't have a large number of "
                    + "items, as all will be held in memory the same time.");
        }
        return errorDescBuilder;
    }

    private static Object[] unexpectedTypeErrorDescription(
            String expectedTypesDesc,
            Expression blamed, String blamedAssignmentTargetVarName,
            TemplateModel model) {
        return new Object[] {
                "Expected ", new _DelayedAOrAn(expectedTypesDesc), ", but ",
                (blamedAssignmentTargetVarName == null
                        ? blamed != null ? "this" : "the expression"
                        : new Object[] {
                                "assignment target variable ",
                                new _DelayedJQuote(blamedAssignmentTargetVarName) }), 
                " has evaluated to ",
                new _DelayedAOrAn(new _DelayedFTLTypeDescription(model)),
                (blamed != null ? ":" : ".")};
    }
    
}
