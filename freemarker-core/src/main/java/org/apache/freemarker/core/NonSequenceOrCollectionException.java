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

package org.apache.freemarker.core;

import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.util._CollectionUtils;

/**
 * Indicates that a {@link TemplateSequenceModel} or {@link TemplateCollectionModel} value was expected, but the value
 * had a different type.
 */
public class NonSequenceOrCollectionException extends UnexpectedTypeException {

    private static final Class[] EXPECTED_TYPES = new Class[] {
        TemplateSequenceModel.class, TemplateCollectionModel.class
    };
    private static final String ITERABLE_SUPPORT_HINT = "The problematic value is a java.lang.Iterable. Using "
            + "DefaultObjectWrapper(..., iterableSupport=true) as the objectWrapper setting of the FreeMarker "
            + "configuration should solve this.";
    
    public NonSequenceOrCollectionException(Environment env) {
        super(env, "Expecting sequence or collection value here");
    }

    public NonSequenceOrCollectionException(String description, Environment env) {
        super(env, description);
    }

    NonSequenceOrCollectionException(Environment env, _ErrorDescriptionBuilder description) {
        super(env, description);
    }

    NonSequenceOrCollectionException(
            ASTExpression blamed, TemplateModel model, Environment env)
            throws InvalidReferenceException {
        this(blamed, model, _CollectionUtils.EMPTY_OBJECT_ARRAY, env);
    }

    NonSequenceOrCollectionException(
            ASTExpression blamed, TemplateModel model, String tip,
            Environment env)
            throws InvalidReferenceException {
        this(blamed, model, new Object[] { tip }, env);
    }

    NonSequenceOrCollectionException(
            ASTExpression blamed, TemplateModel model, Object[] tips, Environment env) throws InvalidReferenceException {
        super(blamed, model, "sequence or collection", EXPECTED_TYPES, extendTipsIfIterable(model, tips), env);
    }
    
    private static Object[] extendTipsIfIterable(TemplateModel model, Object[] tips) {
        if (isWrappedIterable(model)) {
            final int tipsLen = tips != null ? tips.length : 0;
            Object[] extendedTips = new Object[tipsLen + 1];
            for (int i = 0; i < tipsLen; i++) {
                extendedTips[i] = tips[i];
            }
            extendedTips[tipsLen] = ITERABLE_SUPPORT_HINT;
            return extendedTips;
        } else {
            return tips;
        }
    }

    public static boolean isWrappedIterable(TemplateModel model) {
        return model instanceof WrapperTemplateModel
                && ((WrapperTemplateModel) model).getWrappedObject() instanceof Iterable;
    }

}
